package com.eventhub.seat.consumer;

import com.eventhub.seat.service.SeatInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka consumer for seat commands published by the Booking Service saga.
 *
 * Handles:
 * - seats.confirm  → Mark locked seats as BOOKED after successful payment
 * - seats.release  → Release locked seats back to AVAILABLE (compensation)
 * - seats.cancel   → Cancel booked seats back to AVAILABLE (refund/cancellation)
 *
 * Idempotency: Uses a processed-command set keyed by bookingId+commandType
 * to prevent duplicate processing on Kafka redelivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatCommandConsumer {

    private final SeatInventoryService seatInventoryService;

    // In-memory idempotency guard (production: use Redis or DB)
    private final Set<String> processedCommands = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "seat-commands",
            groupId = "seat-inventory-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSeatCommand(Map<String, Object> command) {
        String commandType = getString(command, "commandType");
        String bookingId = getString(command, "bookingId");
        String eventIdStr = getString(command, "eventId");
        String userIdStr = getString(command, "userId");

        if (commandType == null || bookingId == null) {
            log.warn("Received seat command with missing fields: {}", command);
            return;
        }

        // Idempotency check
        String idempotencyKey = bookingId + ":" + commandType;
        if (!processedCommands.add(idempotencyKey)) {
            log.info("Duplicate seat command ignored: {} for booking {}", commandType, bookingId);
            return;
        }

        log.info("Processing seat command: type={} bookingId={} eventId={}", commandType, bookingId, eventIdStr);

        try {
            switch (commandType) {
                case "seats.confirm" -> handleConfirm(command, eventIdStr, userIdStr, bookingId);
                case "seats.release" -> handleRelease(command, eventIdStr, userIdStr);
                case "seats.cancel" -> handleCancel(command, eventIdStr, bookingId);
                default -> log.warn("Unknown seat command type: {}", commandType);
            }
        } catch (Exception e) {
            log.error("Failed to process seat command: type={} bookingId={} error={}",
                    commandType, bookingId, e.getMessage(), e);
            // Remove from processed set so it can be retried
            processedCommands.remove(idempotencyKey);
            throw e; // Let Kafka retry
        }
    }

    private void handleConfirm(Map<String, Object> command, String eventIdStr, String userIdStr, String bookingId) {
        UUID eventId = UUID.fromString(eventIdStr);
        UUID userId = UUID.fromString(userIdStr);
        UUID bookingUuid = UUID.fromString(bookingId);
        List<UUID> seatIds = parseSeatIds(command);

        log.info("Confirming {} seats for booking {} on event {}", seatIds.size(), bookingId, eventIdStr);
        seatInventoryService.confirmSeats(eventId, seatIds, userId, bookingUuid);
    }

    private void handleRelease(Map<String, Object> command, String eventIdStr, String userIdStr) {
        UUID eventId = UUID.fromString(eventIdStr);
        UUID userId = UUID.fromString(userIdStr);
        List<UUID> seatIds = parseSeatIds(command);

        log.info("Releasing {} seats for user {} on event {}", seatIds.size(), userIdStr, eventIdStr);
        seatInventoryService.releaseSeats(eventId, seatIds, userId);
    }

    private void handleCancel(Map<String, Object> command, String eventIdStr, String bookingId) {
        UUID eventId = UUID.fromString(eventIdStr);
        UUID bookingUuid = UUID.fromString(bookingId);
        List<UUID> seatIds = parseSeatIds(command);

        log.info("Cancelling {} seats for booking {} on event {}", seatIds.size(), bookingId, eventIdStr);
        seatInventoryService.cancelSeats(eventId, seatIds, bookingUuid);
    }

    @SuppressWarnings("unchecked")
    private List<UUID> parseSeatIds(Map<String, Object> command) {
        Object seatIdsObj = command.get("seatIds");
        if (seatIdsObj instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(UUID::fromString)
                    .toList();
        }
        throw new IllegalArgumentException("seatIds is missing or not a list");
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
