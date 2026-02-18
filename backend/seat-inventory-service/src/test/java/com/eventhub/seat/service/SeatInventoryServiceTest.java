package com.eventhub.seat.service;

import com.eventhub.seat.entity.Seat;
import com.eventhub.seat.entity.SeatStatus;
import com.eventhub.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatInventoryServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SeatInventoryService service;

    private UUID eventId;
    private UUID userId;
    private UUID seatId1;
    private UUID seatId2;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        seatId1 = UUID.randomUUID();
        seatId2 = UUID.randomUUID();

        ReflectionTestUtils.setField(service, "lockTtlSeconds", 600);
        ReflectionTestUtils.setField(service, "maxSeatsPerUser", 10);

        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private Seat buildSeat(UUID id, SeatStatus status) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setEventId(eventId);
        seat.setSectionId("VIP");
        seat.setRowLabel("A");
        seat.setSeatNumber(1);
        seat.setPrice(BigDecimal.valueOf(100));
        seat.setStatus(status);
        return seat;
    }

    // ─────────────────────────────────────────────
    // getAvailability
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailability")
    class GetAvailabilityTests {

        @Test
        @DisplayName("should return section-level availability with counts")
        void shouldReturnAvailability() {
            Seat s1 = buildSeat(seatId1, SeatStatus.AVAILABLE);
            Seat s2 = buildSeat(seatId2, SeatStatus.BOOKED);
            when(seatRepository.findByEventId(eventId)).thenReturn(List.of(s1, s2));

            Map<String, Object> result = service.getAvailability(eventId);

            assertThat(result).containsKey("sections");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections = (List<Map<String, Object>>) result.get("sections");
            assertThat(sections).hasSize(1);
            assertThat(sections.get(0).get("available")).isEqualTo(1L);
            assertThat(sections.get(0).get("total")).isEqualTo(2L);
        }
    }

    // ─────────────────────────────────────────────
    // lockSeats
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("lockSeats")
    class LockSeatsTests {

        @Test
        @DisplayName("should lock seats in Redis and DB")
        void shouldLockSeatsSuccessfully() {
            List<UUID> seatIds = List.of(seatId1);
            RedisLockService.LockResult lockResult = new RedisLockService.LockResult(true, "lock-123", null);
            when(redisLockService.lockSeats(eventId, seatIds, userId, 600, 10)).thenReturn(lockResult);

            Seat seat = buildSeat(seatId1, SeatStatus.AVAILABLE);
            when(seatRepository.findByEventIdAndIdIn(eventId, seatIds)).thenReturn(List.of(seat));
            when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = service.lockSeats(eventId, seatIds, userId);

            assertThat(result.get("lockId")).isEqualTo("lock-123");
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.LOCKED);
            assertThat(seat.getLockedBy()).isEqualTo(userId);
            verify(kafkaTemplate).send(eq("seat-events"), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("should throw if Redis lock fails")
        void shouldThrowIfRedisLockFails() {
            List<UUID> seatIds = List.of(seatId1);
            RedisLockService.LockResult lockResult = new RedisLockService.LockResult(false, null, "Already locked");
            when(redisLockService.lockSeats(eventId, seatIds, userId, 600, 10)).thenReturn(lockResult);

            assertThatThrownBy(() -> service.lockSeats(eventId, seatIds, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already locked");
        }

        @Test
        @DisplayName("should rollback Redis lock if DB seat not available")
        void shouldRollbackRedisIfDbConflict() {
            List<UUID> seatIds = List.of(seatId1);
            RedisLockService.LockResult lockResult = new RedisLockService.LockResult(true, "lock-456", null);
            when(redisLockService.lockSeats(eventId, seatIds, userId, 600, 10)).thenReturn(lockResult);

            Seat seat = buildSeat(seatId1, SeatStatus.BOOKED);
            when(seatRepository.findByEventIdAndIdIn(eventId, seatIds)).thenReturn(List.of(seat));

            assertThatThrownBy(() -> service.lockSeats(eventId, seatIds, userId))
                    .isInstanceOf(IllegalStateException.class);
            verify(redisLockService).releaseSeats(eventId, seatIds, userId);
        }
    }

    // ─────────────────────────────────────────────
    // confirmSeats
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("confirmSeats")
    class ConfirmSeatsTests {

        @Test
        @DisplayName("should transition locked seats to BOOKED")
        void shouldConfirmSeats() {
            UUID bookingId = UUID.randomUUID();
            List<UUID> seatIds = List.of(seatId1);
            Seat seat = buildSeat(seatId1, SeatStatus.LOCKED);
            seat.setLockedBy(userId);
            when(seatRepository.findByEventIdAndIdIn(eventId, seatIds)).thenReturn(List.of(seat));
            when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            service.confirmSeats(eventId, seatIds, userId, bookingId);

            assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(seat.getBookedBy()).isEqualTo(userId);
            assertThat(seat.getBookingId()).isEqualTo(bookingId);
            assertThat(seat.getLockedBy()).isNull();
            verify(redisLockService).releaseSeats(eventId, seatIds, userId);
        }

        @Test
        @DisplayName("should throw if seat not locked by user")
        void shouldThrowIfNotLockedByUser() {
            UUID bookingId = UUID.randomUUID();
            List<UUID> seatIds = List.of(seatId1);
            Seat seat = buildSeat(seatId1, SeatStatus.LOCKED);
            seat.setLockedBy(UUID.randomUUID()); // different user
            when(seatRepository.findByEventIdAndIdIn(eventId, seatIds)).thenReturn(List.of(seat));

            assertThatThrownBy(() -> service.confirmSeats(eventId, seatIds, userId, bookingId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────────────────────────
    // cancelSeats
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("cancelSeats")
    class CancelSeatsTests {

        @Test
        @DisplayName("should reset seats to AVAILABLE")
        void shouldCancelSeats() {
            UUID bookingId = UUID.randomUUID();
            List<UUID> seatIds = List.of(seatId1);
            Seat seat = buildSeat(seatId1, SeatStatus.BOOKED);
            seat.setBookedBy(userId);
            seat.setBookingId(bookingId);
            when(seatRepository.findByEventIdAndIdIn(eventId, seatIds)).thenReturn(List.of(seat));
            when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            service.cancelSeats(eventId, seatIds, bookingId);

            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(seat.getBookedBy()).isNull();
            assertThat(seat.getBookingId()).isNull();
        }
    }

    // ─────────────────────────────────────────────
    // cleanupExpiredLocks
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("cleanupExpiredLocks")
    class CleanupTests {

        @Test
        @DisplayName("should call repository to release expired locks")
        void shouldCleanupExpired() {
            when(seatRepository.releaseExpiredLocks(any(Instant.class))).thenReturn(3);

            service.cleanupExpiredLocks();

            verify(seatRepository).releaseExpiredLocks(any(Instant.class));
        }
    }
}
