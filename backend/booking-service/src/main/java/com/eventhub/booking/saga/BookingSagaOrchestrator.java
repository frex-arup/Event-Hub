package com.eventhub.booking.saga;

import com.eventhub.booking.entity.Booking;
import com.eventhub.booking.entity.BookingStatus;
import com.eventhub.booking.entity.SagaState;
import com.eventhub.booking.repository.BookingRepository;
import com.eventhub.booking.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga Orchestrator for the booking workflow.
 *
 * Flow:
 * 1. INITIATED      → Create booking record, verify seats locked
 * 2. SEATS_LOCKED   → Request payment
 * 3. PAYMENT_PENDING → Wait for payment callback
 * 4. PAYMENT_COMPLETED → Confirm seats, issue ticket
 * 5. TICKET_ISSUED  → Send notifications
 * 6. COMPLETED      → Done
 *
 * Compensation (on failure at any step):
 * - Release seat locks
 * - Cancel payment (if initiated)
 * - Update booking status to CANCELLED/FAILED
 * - Notify user
 *
 * Idempotency: Uses idempotency_key to prevent duplicate bookings.
 * Exactly-once: Kafka producer idempotence + DB unique constraint on idempotency_key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final QrCodeService qrCodeService;

    // ─────────────────────────────────────────────
    // Step 1: Initiate booking (idempotent)
    // ─────────────────────────────────────────────

    @Transactional
    public Booking initiateBooking(Booking booking) {
        // Idempotency check — return existing if same key
        return bookingRepository.findByIdempotencyKey(booking.getIdempotencyKey())
                .orElseGet(() -> {
                    booking.setSagaState(SagaState.SEATS_LOCKED);
                    booking.setExpiresAt(Instant.now().plusSeconds(600)); // 10 min expiry
                    Booking saved = bookingRepository.save(booking);

                    log.info("Saga INITIATED: booking={} event={} user={}",
                            saved.getId(), saved.getEventId(), saved.getUserId());

                    // Publish booking.requested event
                    publishSagaEvent("booking.requested", saved);
                    return saved;
                });
    }

    // ─────────────────────────────────────────────
    // Step 2: Request payment
    // ─────────────────────────────────────────────

    @Transactional
    public void requestPayment(UUID bookingId, String gateway, String returnUrl) {
        Booking booking = getBookingOrThrow(bookingId);

        if (booking.getSagaState() != SagaState.SEATS_LOCKED) {
            log.warn("Cannot request payment: booking {} is in state {}", bookingId, booking.getSagaState());
            throw new IllegalStateException("Booking is not in the correct state for payment");
        }

        booking.setSagaState(SagaState.PAYMENT_PENDING);
        bookingRepository.save(booking);

        // Publish payment.initiate event
        kafkaTemplate.send("payment-commands", booking.getId().toString(), Map.of(
                "commandType", "payment.initiate",
                "bookingId", booking.getId().toString(),
                "userId", booking.getUserId().toString(),
                "amount", booking.getTotalAmount().toString(),
                "currency", booking.getCurrency(),
                "gateway", gateway,
                "returnUrl", returnUrl,
                "idempotencyKey", booking.getIdempotencyKey(),
                "timestamp", Instant.now().toString()
        ));

        log.info("Saga PAYMENT_PENDING: booking={}", bookingId);
    }

    // ─────────────────────────────────────────────
    // Step 3: Handle payment result (from Kafka)
    // ─────────────────────────────────────────────

    @Transactional
    public void handlePaymentSuccess(UUID bookingId, UUID paymentId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (booking.getSagaState() != SagaState.PAYMENT_PENDING &&
            booking.getSagaState() != SagaState.PAYMENT_PROCESSING) {
            log.warn("Payment success for booking {} but state is {}", bookingId, booking.getSagaState());
            return;
        }

        booking.setSagaState(SagaState.PAYMENT_COMPLETED);
        booking.setPaymentId(paymentId);
        bookingRepository.save(booking);

        log.info("Saga PAYMENT_COMPLETED: booking={} payment={}", bookingId, paymentId);

        // Proceed to confirm seats
        confirmBooking(bookingId);
    }

    @Transactional
    public void handlePaymentFailure(UUID bookingId, String reason) {
        Booking booking = getBookingOrThrow(bookingId);

        log.warn("Saga PAYMENT_FAILED: booking={} reason={}", bookingId, reason);

        // Trigger compensation
        compensate(booking, "Payment failed: " + reason);
    }

    // ─────────────────────────────────────────────
    // Step 4: Confirm booking → issue ticket
    // ─────────────────────────────────────────────

    @Transactional
    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (booking.getSagaState() != SagaState.PAYMENT_COMPLETED) {
            log.warn("Cannot confirm booking {}: state is {}", bookingId, booking.getSagaState());
            return;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSagaState(SagaState.TICKET_ISSUED);
        booking.setConfirmedAt(Instant.now());
        booking.setQrCode(generateQrCode(booking));
        bookingRepository.save(booking);

        // Tell seat service to finalize seats
        List<String> seatIds = booking.getSeats().stream()
                .map(s -> s.getSeatId().toString())
                .toList();

        kafkaTemplate.send("seat-commands", booking.getEventId().toString(), Map.of(
                "commandType", "seats.confirm",
                "eventId", booking.getEventId().toString(),
                "bookingId", booking.getId().toString(),
                "userId", booking.getUserId().toString(),
                "seatIds", seatIds,
                "timestamp", Instant.now().toString()
        ));

        // Issue notification
        kafkaTemplate.send("notification-events", booking.getUserId().toString(), Map.of(
                "eventType", "booking.confirmed",
                "bookingId", booking.getId().toString(),
                "userId", booking.getUserId().toString(),
                "eventId", booking.getEventId().toString(),
                "totalAmount", booking.getTotalAmount().toString(),
                "currency", booking.getCurrency(),
                "seatCount", booking.getSeats().size(),
                "timestamp", Instant.now().toString()
        ));

        log.info("Saga COMPLETED: booking={} confirmed with {} seats",
                bookingId, booking.getSeats().size());

        booking.setSagaState(SagaState.COMPLETED);
        bookingRepository.save(booking);
    }

    // ─────────────────────────────────────────────
    // Compensation (rollback)
    // ─────────────────────────────────────────────

    @Transactional
    public void compensate(Booking booking, String reason) {
        log.warn("Saga COMPENSATING: booking={} reason={}", booking.getId(), reason);

        booking.setSagaState(SagaState.COMPENSATING);
        booking.setFailureReason(reason);
        bookingRepository.save(booking);

        // Release seats
        List<String> seatIds = booking.getSeats().stream()
                .map(s -> s.getSeatId().toString())
                .toList();

        if (!seatIds.isEmpty()) {
            kafkaTemplate.send("seat-commands", booking.getEventId().toString(), Map.of(
                    "commandType", "seats.release",
                    "eventId", booking.getEventId().toString(),
                    "userId", booking.getUserId().toString(),
                    "seatIds", seatIds,
                    "bookingId", booking.getId().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        // Cancel payment if initiated
        if (booking.getPaymentId() != null) {
            kafkaTemplate.send("payment-commands", booking.getId().toString(), Map.of(
                    "commandType", "payment.cancel",
                    "bookingId", booking.getId().toString(),
                    "paymentId", booking.getPaymentId().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        // Notify user
        kafkaTemplate.send("notification-events", booking.getUserId().toString(), Map.of(
                "eventType", "booking.failed",
                "bookingId", booking.getId().toString(),
                "userId", booking.getUserId().toString(),
                "reason", reason,
                "timestamp", Instant.now().toString()
        ));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setSagaState(SagaState.COMPENSATION_COMPLETED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        log.info("Saga COMPENSATION_COMPLETED: booking={}", booking.getId());
    }

    // ─────────────────────────────────────────────
    // Cancel booking (user-initiated)
    // ─────────────────────────────────────────────

    @Transactional
    public void cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            // Need refund flow
            kafkaTemplate.send("payment-commands", booking.getId().toString(), Map.of(
                    "commandType", "payment.refund",
                    "bookingId", booking.getId().toString(),
                    "paymentId", booking.getPaymentId() != null ? booking.getPaymentId().toString() : "",
                    "amount", booking.getTotalAmount().toString(),
                    "currency", booking.getCurrency(),
                    "timestamp", Instant.now().toString()
            ));
        }

        compensate(booking, "Cancelled by user");
    }

    // ─────────────────────────────────────────────
    // Scheduled: Expire stale bookings
    // ─────────────────────────────────────────────

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireStaleBookings() {
        List<Booking> expired = bookingRepository.findExpiredBookings(
                SagaState.PAYMENT_PENDING, Instant.now()
        );

        for (Booking booking : expired) {
            log.info("Expiring stale booking: {}", booking.getId());
            compensate(booking, "Payment timeout — booking expired");
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} stale bookings", expired.size());
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private Booking getBookingOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    private String generateQrCode(Booking booking) {
        return qrCodeService.generateBookingQrCode(
                booking.getId(), booking.getEventId(),
                booking.getUserId(), booking.getSeats().size());
    }

    private void publishSagaEvent(String eventType, Booking booking) {
        try {
            kafkaTemplate.send("booking-events", booking.getId().toString(), Map.of(
                    "eventType", eventType,
                    "bookingId", booking.getId().toString(),
                    "eventId", booking.getEventId().toString(),
                    "userId", booking.getUserId().toString(),
                    "sagaState", booking.getSagaState().name(),
                    "totalAmount", booking.getTotalAmount().toString(),
                    "currency", booking.getCurrency(),
                    "seatCount", booking.getSeats().size(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish saga event: {}", e.getMessage());
        }
    }
}
