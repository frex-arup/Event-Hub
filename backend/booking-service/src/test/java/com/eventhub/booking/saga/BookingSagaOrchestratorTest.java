package com.eventhub.booking.saga;

import com.eventhub.booking.entity.*;
import com.eventhub.booking.repository.BookingRepository;
import com.eventhub.booking.service.QrCodeService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSagaOrchestratorTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private BookingSagaOrchestrator orchestrator;

    @Captor
    private ArgumentCaptor<Booking> bookingCaptor;

    private Booking testBooking;
    private UUID bookingId;
    private UUID eventId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();

        BookedSeat seat = BookedSeat.builder()
                .id(UUID.randomUUID())
                .seatId(UUID.randomUUID())
                .sectionName("A")
                .rowLabel("A")
                .seatNumber(1)
                .price(BigDecimal.valueOf(50))
                .currency("USD")
                .build();

        testBooking = Booking.builder()
                .id(bookingId)
                .eventId(eventId)
                .userId(userId)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(50))
                .currency("USD")
                .idempotencyKey("idem-key-123")
                .sagaState(SagaState.INITIATED)
                .seats(new ArrayList<>(List.of(seat)))
                .build();

        seat.setBooking(testBooking);

        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // ─────────────────────────────────────────────
    // Step 1: Initiate Booking
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("initiateBooking")
    class InitiateBookingTests {

        @Test
        @DisplayName("should create new booking and set saga state to SEATS_LOCKED")
        void shouldCreateNewBooking() {
            when(bookingRepository.findByIdempotencyKey("idem-key-123"))
                    .thenReturn(Optional.empty());
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Booking result = orchestrator.initiateBooking(testBooking);

            assertThat(result.getSagaState()).isEqualTo(SagaState.SEATS_LOCKED);
            assertThat(result.getExpiresAt()).isAfter(Instant.now());
            verify(kafkaTemplate).send(eq("booking-events"), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("should return existing booking for duplicate idempotency key")
        void shouldReturnExistingForDuplicateKey() {
            testBooking.setSagaState(SagaState.SEATS_LOCKED);
            when(bookingRepository.findByIdempotencyKey("idem-key-123"))
                    .thenReturn(Optional.of(testBooking));

            Booking result = orchestrator.initiateBooking(testBooking);

            assertThat(result.getId()).isEqualTo(bookingId);
            verify(bookingRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // Step 2: Request Payment
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("requestPayment")
    class RequestPaymentTests {

        @Test
        @DisplayName("should transition to PAYMENT_PENDING and send payment command")
        void shouldRequestPayment() {
            testBooking.setSagaState(SagaState.SEATS_LOCKED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.requestPayment(bookingId, "stripe", "http://return.url");

            verify(bookingRepository).save(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getSagaState()).isEqualTo(SagaState.PAYMENT_PENDING);
            verify(kafkaTemplate).send(eq("payment-commands"), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("should throw if booking is not in SEATS_LOCKED state")
        void shouldThrowIfWrongState() {
            testBooking.setSagaState(SagaState.INITIATED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> orchestrator.requestPayment(bookingId, "stripe", "http://return.url"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in the correct state");
        }

        @Test
        @DisplayName("should throw if booking not found")
        void shouldThrowIfNotFound() {
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orchestrator.requestPayment(bookingId, "stripe", "http://return.url"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────
    // Step 3: Handle Payment Result
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("handlePaymentSuccess")
    class HandlePaymentSuccessTests {

        @Test
        @DisplayName("should set PAYMENT_COMPLETED and proceed to confirm")
        void shouldHandlePaymentSuccess() {
            testBooking.setSagaState(SagaState.PAYMENT_PENDING);
            UUID paymentId = UUID.randomUUID();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(qrCodeService.generateBookingQrCode(any(), any(), any(), anyInt()))
                    .thenReturn("data:image/png;base64,MOCK_QR");

            orchestrator.handlePaymentSuccess(bookingId, paymentId);

            // Should save multiple times: PAYMENT_COMPLETED, then TICKET_ISSUED, then COMPLETED
            verify(bookingRepository, atLeast(2)).save(bookingCaptor.capture());
            List<Booking> savedBookings = bookingCaptor.getAllValues();
            Booking lastSave = savedBookings.get(savedBookings.size() - 1);

            assertThat(lastSave.getSagaState()).isEqualTo(SagaState.COMPLETED);
            assertThat(lastSave.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(lastSave.getPaymentId()).isEqualTo(paymentId);
        }

        @Test
        @DisplayName("should ignore payment success if not in PAYMENT_PENDING state")
        void shouldIgnoreIfWrongState() {
            testBooking.setSagaState(SagaState.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            orchestrator.handlePaymentSuccess(bookingId, UUID.randomUUID());

            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("handlePaymentFailure")
    class HandlePaymentFailureTests {

        @Test
        @DisplayName("should trigger compensation on payment failure")
        void shouldCompensateOnFailure() {
            testBooking.setSagaState(SagaState.PAYMENT_PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.handlePaymentFailure(bookingId, "Insufficient funds");

            verify(bookingRepository, atLeast(2)).save(bookingCaptor.capture());
            Booking lastSave = bookingCaptor.getAllValues().get(bookingCaptor.getAllValues().size() - 1);
            assertThat(lastSave.getSagaState()).isEqualTo(SagaState.COMPENSATION_COMPLETED);
            assertThat(lastSave.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    // ─────────────────────────────────────────────
    // Step 4: Confirm Booking
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("confirmBooking")
    class ConfirmBookingTests {

        @Test
        @DisplayName("should generate QR code, confirm seats, and notify")
        void shouldConfirmBooking() {
            testBooking.setSagaState(SagaState.PAYMENT_COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(qrCodeService.generateBookingQrCode(bookingId, eventId, userId, 1))
                    .thenReturn("data:image/png;base64,MOCK_QR");

            orchestrator.confirmBooking(bookingId);

            verify(bookingRepository, atLeast(1)).save(bookingCaptor.capture());
            Booking lastSave = bookingCaptor.getAllValues().get(bookingCaptor.getAllValues().size() - 1);

            assertThat(lastSave.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(lastSave.getSagaState()).isEqualTo(SagaState.COMPLETED);
            assertThat(lastSave.getQrCode()).isEqualTo("data:image/png;base64,MOCK_QR");
            assertThat(lastSave.getConfirmedAt()).isNotNull();

            // Should send seat-commands and notification-events
            verify(kafkaTemplate).send(eq("seat-commands"), anyString(), any(Map.class));
            verify(kafkaTemplate).send(eq("notification-events"), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("should skip confirmation if not in PAYMENT_COMPLETED state")
        void shouldSkipIfNotPaymentCompleted() {
            testBooking.setSagaState(SagaState.SEATS_LOCKED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            orchestrator.confirmBooking(bookingId);

            verify(bookingRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // Compensation
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("compensate")
    class CompensateTests {

        @Test
        @DisplayName("should release seats, cancel payment, notify user")
        void shouldCompensateFully() {
            testBooking.setSagaState(SagaState.PAYMENT_PENDING);
            testBooking.setPaymentId(UUID.randomUUID());
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.compensate(testBooking, "Test failure");

            verify(bookingRepository, atLeast(2)).save(bookingCaptor.capture());
            Booking lastSave = bookingCaptor.getAllValues().get(bookingCaptor.getAllValues().size() - 1);
            assertThat(lastSave.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(lastSave.getSagaState()).isEqualTo(SagaState.COMPENSATION_COMPLETED);
            assertThat(lastSave.getFailureReason()).isEqualTo("Test failure");
            assertThat(lastSave.getCancelledAt()).isNotNull();

            // Should send seat-commands, payment-commands, notification-events
            verify(kafkaTemplate).send(eq("seat-commands"), anyString(), any(Map.class));
            verify(kafkaTemplate).send(eq("payment-commands"), anyString(), any(Map.class));
            verify(kafkaTemplate).send(eq("notification-events"), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("should skip payment cancel if no paymentId")
        void shouldSkipPaymentCancelIfNoPayment() {
            testBooking.setPaymentId(null);
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.compensate(testBooking, "No payment");

            verify(kafkaTemplate, never()).send(eq("payment-commands"), anyString(), any(Map.class));
            verify(kafkaTemplate).send(eq("seat-commands"), anyString(), any(Map.class));
        }
    }

    // ─────────────────────────────────────────────
    // Cancel Booking (user-initiated)
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBookingTests {

        @Test
        @DisplayName("should cancel confirmed booking and request refund")
        void shouldCancelConfirmedBooking() {
            testBooking.setStatus(BookingStatus.CONFIRMED);
            testBooking.setPaymentId(UUID.randomUUID());
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.cancelBooking(bookingId, userId);

            // Should send payment refund command
            verify(kafkaTemplate).send(eq("payment-commands"), eq(bookingId.toString()), argThat(map -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) map;
                return "payment.refund".equals(m.get("commandType"));
            }));
        }

        @Test
        @DisplayName("should throw if user is not the booking owner")
        void shouldThrowIfNotOwner() {
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            UUID otherUser = UUID.randomUUID();

            assertThatThrownBy(() -> orchestrator.cancelBooking(bookingId, otherUser))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Not authorized");
        }
    }

    // ─────────────────────────────────────────────
    // Expire Stale Bookings
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("expireStaleBookings")
    class ExpireStaleBookingsTests {

        @Test
        @DisplayName("should compensate all expired bookings")
        void shouldExpireStaleBookings() {
            Booking expired1 = Booking.builder()
                    .id(UUID.randomUUID()).eventId(eventId).userId(userId)
                    .sagaState(SagaState.PAYMENT_PENDING)
                    .totalAmount(BigDecimal.TEN).currency("USD")
                    .idempotencyKey("exp-1")
                    .seats(new ArrayList<>())
                    .build();

            when(bookingRepository.findExpiredBookings(eq(SagaState.PAYMENT_PENDING), any(Instant.class)))
                    .thenReturn(List.of(expired1));
            when(bookingRepository.save(any(Booking.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            orchestrator.expireStaleBookings();

            verify(bookingRepository, atLeast(1)).save(bookingCaptor.capture());
            Booking lastSave = bookingCaptor.getAllValues().get(bookingCaptor.getAllValues().size() - 1);
            assertThat(lastSave.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should do nothing if no expired bookings")
        void shouldDoNothingIfNoExpired() {
            when(bookingRepository.findExpiredBookings(eq(SagaState.PAYMENT_PENDING), any(Instant.class)))
                    .thenReturn(List.of());

            orchestrator.expireStaleBookings();

            verify(bookingRepository, never()).save(any());
        }
    }
}
