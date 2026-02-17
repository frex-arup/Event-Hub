package com.eventhub.seat.controller;

import com.eventhub.seat.entity.Seat;
import com.eventhub.seat.service.SeatInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatInventoryService seatInventoryService;

    @GetMapping("/availability/{eventId}")
    public ResponseEntity<Map<String, Object>> getAvailability(@PathVariable UUID eventId) {
        return ResponseEntity.ok(seatInventoryService.getAvailability(eventId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Seat>> getSeats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(seatInventoryService.getSeatsForEvent(eventId));
    }

    @PostMapping("/lock")
    public ResponseEntity<Map<String, Object>> lockSeats(
            @Valid @RequestBody SeatLockRequest request,
            @RequestHeader("X-User-Id") String userId) {
        Map<String, Object> result = seatInventoryService.lockSeats(
                request.getEventId(),
                request.getSeatIds(),
                UUID.fromString(userId)
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseSeats(
            @Valid @RequestBody SeatReleaseRequest request,
            @RequestHeader("X-User-Id") String userId) {
        seatInventoryService.releaseSeats(
                request.getEventId(),
                request.getSeatIds(),
                UUID.fromString(userId)
        );
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // Request DTOs
    // ─────────────────────────────────────────────

    @Data
    public static class SeatLockRequest {
        @NotNull
        private UUID eventId;
        @NotEmpty
        private List<UUID> seatIds;
    }

    @Data
    public static class SeatReleaseRequest {
        @NotNull
        private UUID eventId;
        @NotEmpty
        private List<UUID> seatIds;
    }

    // ─────────────────────────────────────────────
    // Exception handlers
    // ─────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
