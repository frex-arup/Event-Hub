package com.eventhub.event.controller;

import com.eventhub.event.entity.EventSession;
import com.eventhub.event.service.EventSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventSessionController {

    private final EventSessionService sessionService;

    @GetMapping("/{eventId}/sessions")
    public ResponseEntity<List<EventSession>> getSessions(@PathVariable UUID eventId) {
        return ResponseEntity.ok(sessionService.getSessionsForEvent(eventId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<EventSession> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @PostMapping("/{eventId}/sessions")
    public ResponseEntity<EventSession> createSession(
            @PathVariable UUID eventId,
            @RequestBody EventSession session,
            @RequestHeader("X-User-Id") String userId) {
        session.setEventId(eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(session, UUID.fromString(userId)));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<EventSession> updateSession(
            @PathVariable UUID sessionId,
            @RequestBody EventSession updates,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, updates, UUID.fromString(userId)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") String userId) {
        sessionService.deleteSession(sessionId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }
}
