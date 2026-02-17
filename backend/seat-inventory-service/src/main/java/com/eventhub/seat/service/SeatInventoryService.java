package com.eventhub.seat.service;

import com.eventhub.seat.entity.Seat;
import com.eventhub.seat.entity.SeatStatus;
import com.eventhub.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatInventoryService {

    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${seat.lock.ttl-seconds:600}")
    private int lockTtlSeconds;

    @Value("${seat.lock.max-seats-per-user:10}")
    private int maxSeatsPerUser;

    // ─────────────────────────────────────────────
    // Seat Availability (cached via Redis)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(UUID eventId) {
        List<Seat> seats = seatRepository.findByEventId(eventId);

        Map<String, List<Seat>> bySections = seats.stream()
                .collect(Collectors.groupingBy(Seat::getSectionId));

        List<Map<String, Object>> sections = new ArrayList<>();
        for (var entry : bySections.entrySet()) {
            long available = entry.getValue().stream()
                    .filter(s -> s.getStatus() == SeatStatus.AVAILABLE || s.isLockExpired())
                    .count();
            long total = entry.getValue().size();
            double price = entry.getValue().stream()
                    .mapToDouble(s -> s.getPrice().doubleValue())
                    .min().orElse(0);

            // Cache in Redis for fast reads
            redisLockService.cacheAvailability(eventId, entry.getKey(), available, total);

            sections.add(Map.of(
                    "sectionId", entry.getKey(),
                    "available", available,
                    "total", total,
                    "price", price
            ));
        }

        return Map.of(
                "eventId", eventId.toString(),
                "sections", sections,
                "lastUpdated", Instant.now().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<Seat> getSeatsForEvent(UUID eventId) {
        return seatRepository.findByEventId(eventId);
    }

    // ─────────────────────────────────────────────
    // Seat Locking (Redis + DB double-write)
    // ─────────────────────────────────────────────

    @Transactional
    public Map<String, Object> lockSeats(UUID eventId, List<UUID> seatIds, UUID userId) {
        // 1. Acquire distributed lock in Redis (atomic, Lua-based)
        RedisLockService.LockResult lockResult = redisLockService.lockSeats(
                eventId, seatIds, userId, lockTtlSeconds, maxSeatsPerUser
        );

        if (!lockResult.success()) {
            throw new IllegalStateException(lockResult.errorMessage());
        }

        // 2. Update DB state (optimistic locking via @Version)
        List<Seat> seats = seatRepository.findByEventIdAndIdIn(eventId, seatIds);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(lockTtlSeconds);

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.AVAILABLE || seat.isLockExpired()) {
                seat.setStatus(SeatStatus.LOCKED);
                seat.setLockedBy(userId);
                seat.setLockedAt(now);
                seat.setLockExpiresAt(expiresAt);
            } else if (seat.getStatus() == SeatStatus.LOCKED && userId.equals(seat.getLockedBy())) {
                // Already locked by same user — extend
                seat.setLockExpiresAt(expiresAt);
            } else {
                // Rollback Redis lock
                redisLockService.releaseSeats(eventId, seatIds, userId);
                throw new IllegalStateException("Seat " + seat.getId() + " is no longer available");
            }
        }
        seatRepository.saveAll(seats);

        // 3. Publish Kafka event
        publishSeatEvent("seat.locked", eventId, seatIds, userId);

        log.info("Locked {} seats for user {} on event {}, expires at {}",
                seatIds.size(), userId, eventId, expiresAt);

        return Map.of(
                "lockId", lockResult.lockId(),
                "seatIds", seatIds.stream().map(UUID::toString).toList(),
                "expiresAt", expiresAt.toString()
        );
    }

    // ─────────────────────────────────────────────
    // Seat Release
    // ─────────────────────────────────────────────

    @Transactional
    public void releaseSeats(UUID eventId, List<UUID> seatIds, UUID userId) {
        // Release from Redis
        redisLockService.releaseSeats(eventId, seatIds, userId);

        // Release from DB
        seatRepository.releaseLocksByUser(seatIds, userId);

        publishSeatEvent("seat.released", eventId, seatIds, userId);
        log.info("Released {} seats for user {} on event {}", seatIds.size(), userId, eventId);
    }

    // ─────────────────────────────────────────────
    // Seat Booking (called by Booking Service via Kafka)
    // ─────────────────────────────────────────────

    @Transactional
    public void confirmSeats(UUID eventId, List<UUID> seatIds, UUID userId, UUID bookingId) {
        List<Seat> seats = seatRepository.findByEventIdAndIdIn(eventId, seatIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.LOCKED || !userId.equals(seat.getLockedBy())) {
                throw new IllegalStateException("Seat " + seat.getId() + " is not locked by user " + userId);
            }
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBookedBy(userId);
            seat.setBookingId(bookingId);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
        }
        seatRepository.saveAll(seats);

        // Release Redis locks (no longer needed)
        redisLockService.releaseSeats(eventId, seatIds, userId);

        publishSeatEvent("seat.booked", eventId, seatIds, userId);
        log.info("Confirmed {} seats for booking {} on event {}", seatIds.size(), bookingId, eventId);
    }

    // ─────────────────────────────────────────────
    // Seat Cancellation (rollback from Saga)
    // ─────────────────────────────────────────────

    @Transactional
    public void cancelSeats(UUID eventId, List<UUID> seatIds, UUID bookingId) {
        List<Seat> seats = seatRepository.findByEventIdAndIdIn(eventId, seatIds);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setBookedBy(null);
            seat.setBookingId(null);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
        }
        seatRepository.saveAll(seats);

        log.info("Cancelled {} seats for booking {} on event {}", seatIds.size(), bookingId, eventId);
    }

    // ─────────────────────────────────────────────
    // Scheduled: Expire stale locks
    // ─────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${seat.lock.cleanup-interval-ms:60000}")
    @Transactional
    public void cleanupExpiredLocks() {
        int released = seatRepository.releaseExpiredLocks(Instant.now());
        if (released > 0) {
            log.info("Cleaned up {} expired seat locks", released);
        }
    }

    // ─────────────────────────────────────────────
    // Kafka publishing
    // ─────────────────────────────────────────────

    private void publishSeatEvent(String eventType, UUID eventId, List<UUID> seatIds, UUID userId) {
        try {
            kafkaTemplate.send("seat-events", eventId.toString(), Map.of(
                    "eventType", eventType,
                    "eventId", eventId.toString(),
                    "seatIds", seatIds.stream().map(UUID::toString).toList(),
                    "userId", userId != null ? userId.toString() : "",
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish seat event: {}", e.getMessage());
        }
    }
}
