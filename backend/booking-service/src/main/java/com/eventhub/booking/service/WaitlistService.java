package com.eventhub.booking.service;

import com.eventhub.booking.entity.WaitlistEntry;
import com.eventhub.booking.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public WaitlistEntry joinWaitlist(UUID eventId, UUID userId, String sectionId, int seatCount) {
        // Idempotent: if already on waitlist, return existing entry
        return waitlistRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> {
                    WaitlistEntry entry = WaitlistEntry.builder()
                            .eventId(eventId)
                            .userId(userId)
                            .sectionId(sectionId)
                            .seatCount(seatCount)
                            .status("WAITING")
                            .build();
                    entry = waitlistRepository.save(entry);
                    log.info("User {} joined waitlist for event {} (section={}, seats={})",
                            userId, eventId, sectionId, seatCount);
                    return entry;
                });
    }

    @Transactional
    public void leaveWaitlist(UUID eventId, UUID userId) {
        waitlistRepository.findByEventIdAndUserId(eventId, userId)
                .ifPresent(entry -> {
                    waitlistRepository.delete(entry);
                    log.info("User {} left waitlist for event {}", userId, eventId);
                });
    }

    @Transactional(readOnly = true)
    public Page<WaitlistEntry> getWaitlistForEvent(UUID eventId, int page, int size) {
        return waitlistRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventId, "WAITING", PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<WaitlistEntry> getUserWaitlistEntries(UUID userId, int page, int size) {
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long getWaitlistPosition(UUID eventId, UUID userId) {
        return waitlistRepository.findByEventIdAndUserId(eventId, userId)
                .map(entry -> {
                    // Count entries created before this one
                    return waitlistRepository.countByEventIdAndStatus(eventId, "WAITING");
                })
                .orElse(0L);
    }

    /**
     * Called when seats become available for an event.
     * Notifies the next users in the waitlist.
     */
    @Transactional
    public void notifyNextInWaitlist(UUID eventId, int availableSeats) {
        List<WaitlistEntry> entries = waitlistRepository
                .findTop10ByEventIdAndStatusOrderByCreatedAtAsc(eventId, "WAITING");

        for (WaitlistEntry entry : entries) {
            if (availableSeats <= 0) break;
            if (entry.getSeatCount() <= availableSeats) {
                entry.setStatus("NOTIFIED");
                entry.setNotifiedAt(Instant.now());
                waitlistRepository.save(entry);
                availableSeats -= entry.getSeatCount();

                // Send notification
                try {
                    kafkaTemplate.send("notification-events", entry.getUserId().toString(), Map.of(
                            "eventType", "waitlist.available",
                            "userId", entry.getUserId().toString(),
                            "eventId", eventId.toString(),
                            "seatCount", entry.getSeatCount(),
                            "timestamp", Instant.now().toString()
                    ));
                } catch (Exception e) {
                    log.warn("Failed to send waitlist notification: {}", e.getMessage());
                }

                log.info("Notified waitlist user {} for event {} ({} seats)",
                        entry.getUserId(), eventId, entry.getSeatCount());
            }
        }
    }
}
