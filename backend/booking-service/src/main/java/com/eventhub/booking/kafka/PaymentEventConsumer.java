package com.eventhub.booking.kafka;

import com.eventhub.booking.saga.BookingSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes payment result events from the payment service
 * and drives the booking saga forward or triggers compensation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final BookingSagaOrchestrator sagaOrchestrator;

    @KafkaListener(topics = "payment-events", groupId = "booking-service-group")
    public void handlePaymentEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String bookingIdStr = (String) event.get("bookingId");

        if (eventType == null || bookingIdStr == null) {
            log.warn("Received invalid payment event: {}", event);
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);

        try {
            switch (eventType) {
                case "payment.success" -> {
                    String paymentIdStr = (String) event.get("paymentId");
                    UUID paymentId = UUID.fromString(paymentIdStr);
                    log.info("Payment success for booking {}: payment={}", bookingId, paymentId);
                    sagaOrchestrator.handlePaymentSuccess(bookingId, paymentId);
                }
                case "payment.failed" -> {
                    String reason = (String) event.getOrDefault("reason", "Unknown payment failure");
                    log.warn("Payment failed for booking {}: {}", bookingId, reason);
                    sagaOrchestrator.handlePaymentFailure(bookingId, reason);
                }
                case "payment.refunded" -> {
                    log.info("Payment refunded for booking {}", bookingId);
                    // Refund handling already done in compensation
                }
                default -> log.debug("Ignoring payment event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing payment event for booking {}: {}", bookingId, e.getMessage(), e);
            // DLQ handling: in production, send to a dead letter topic
        }
    }
}
