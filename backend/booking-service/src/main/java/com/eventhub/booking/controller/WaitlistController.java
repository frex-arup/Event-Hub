package com.eventhub.booking.controller;

import com.eventhub.booking.entity.WaitlistEntry;
import com.eventhub.booking.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/{eventId}")
    public ResponseEntity<WaitlistEntry> joinWaitlist(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) Map<String, Object> body) {
        String sectionId = body != null ? (String) body.get("sectionId") : null;
        int seatCount = body != null && body.containsKey("seatCount")
                ? ((Number) body.get("seatCount")).intValue() : 1;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waitlistService.joinWaitlist(eventId, UUID.fromString(userId), sectionId, seatCount));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> leaveWaitlist(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId) {
        waitlistService.leaveWaitlist(eventId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<Page<WaitlistEntry>> getWaitlist(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(waitlistService.getWaitlistForEvent(eventId, page, size));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<WaitlistEntry>> getMyWaitlistEntries(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(waitlistService.getUserWaitlistEntries(UUID.fromString(userId), page, size));
    }

    @GetMapping("/{eventId}/position")
    public ResponseEntity<Map<String, Long>> getPosition(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId) {
        long position = waitlistService.getWaitlistPosition(eventId, UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("position", position));
    }
}
