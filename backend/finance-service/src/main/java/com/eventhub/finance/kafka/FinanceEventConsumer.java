package com.eventhub.finance.kafka;

import com.eventhub.finance.entity.RevenueRecord;
import com.eventhub.finance.repository.RevenueRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceEventConsumer {

    private final RevenueRecordRepository revenueRepository;

    @KafkaListener(topics = "booking-events", groupId = "finance-service-group")
    public void handleBookingEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (eventType == null) return;

        try {
            switch (eventType) {
                case "booking.requested" -> recordTicketSale(event);
                case "booking.refunded" -> recordRefund(event);
                default -> log.trace("Unhandled booking event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing finance event: {}", e.getMessage(), e);
        }
    }

    private void recordTicketSale(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        String userId = (String) event.get("userId");
        String bookingId = (String) event.get("bookingId");
        String amount = (String) event.get("totalAmount");
        String currency = (String) event.getOrDefault("currency", "USD");

        if (eventId == null || amount == null) return;

        RevenueRecord record = RevenueRecord.builder()
                .eventId(UUID.fromString(eventId))
                .organizerId(UUID.fromString(userId)) // Will be resolved to organizer later
                .bookingId(bookingId != null ? UUID.fromString(bookingId) : null)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .type("TICKET_SALE")
                .build();

        revenueRepository.save(record);
        log.info("Revenue recorded: {} {} for event {}", currency, amount, eventId);
    }

    private void recordRefund(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        String userId = (String) event.get("userId");
        String bookingId = (String) event.get("bookingId");
        String amount = (String) event.get("totalAmount");
        String currency = (String) event.getOrDefault("currency", "USD");

        if (eventId == null || amount == null) return;

        RevenueRecord record = RevenueRecord.builder()
                .eventId(UUID.fromString(eventId))
                .organizerId(UUID.fromString(userId))
                .bookingId(bookingId != null ? UUID.fromString(bookingId) : null)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .type("REFUND")
                .build();

        revenueRepository.save(record);
        log.info("Refund recorded: {} {} for event {}", currency, amount, eventId);
    }
}
