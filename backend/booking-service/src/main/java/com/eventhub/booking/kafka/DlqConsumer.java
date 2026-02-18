package com.eventhub.booking.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumes messages from Dead Letter Queue (DLQ) topics for monitoring,
 * alerting, and manual retry. Failed messages that land in DLQs are logged
 * with full context and can be replayed to their original topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DlqConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final AtomicLong bookingDlqCount = new AtomicLong(0);
    private final AtomicLong paymentDlqCount = new AtomicLong(0);
    private final AtomicLong seatDlqCount = new AtomicLong(0);

    @KafkaListener(topics = "booking-events-dlq", groupId = "dlq-consumer-group")
    public void handleBookingDlq(Map<String, Object> message) {
        long count = bookingDlqCount.incrementAndGet();
        log.error("[DLQ] booking-events-dlq message #{}: eventType={}, bookingId={}, payload={}",
                count,
                message.get("eventType"),
                message.get("bookingId"),
                message);
    }

    @KafkaListener(topics = "payment-events-dlq", groupId = "dlq-consumer-group")
    public void handlePaymentDlq(Map<String, Object> message) {
        long count = paymentDlqCount.incrementAndGet();
        log.error("[DLQ] payment-events-dlq message #{}: eventType={}, bookingId={}, paymentId={}, payload={}",
                count,
                message.get("eventType"),
                message.get("bookingId"),
                message.get("paymentId"),
                message);
    }

    @KafkaListener(topics = "seat-events-dlq", groupId = "dlq-consumer-group")
    public void handleSeatDlq(Map<String, Object> message) {
        long count = seatDlqCount.incrementAndGet();
        log.error("[DLQ] seat-events-dlq message #{}: eventType={}, eventId={}, payload={}",
                count,
                message.get("eventType"),
                message.get("eventId"),
                message);
    }

    /**
     * Replay a DLQ message back to its original topic for retry.
     *
     * @param originalTopic the original topic name (e.g., "booking-events")
     * @param key           the message key
     * @param message       the original message payload
     */
    public void replay(String originalTopic, String key, Map<String, Object> message) {
        log.info("[DLQ] Replaying message to topic={} key={}", originalTopic, key);
        message.put("_dlq_replayed_at", Instant.now().toString());
        kafkaTemplate.send(originalTopic, key, message);
    }

    public long getBookingDlqCount() { return bookingDlqCount.get(); }
    public long getPaymentDlqCount() { return paymentDlqCount.get(); }
    public long getSeatDlqCount() { return seatDlqCount.get(); }
}
