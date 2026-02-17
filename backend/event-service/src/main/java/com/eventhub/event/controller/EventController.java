package com.eventhub.event.controller;

import com.eventhub.event.entity.Event;
import com.eventhub.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<Event>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(eventService.getEvents(page, size, category, search));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<Event> getEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Event>> getTrendingEvents() {
        return ResponseEntity.ok(eventService.getTrendingEvents());
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<Page<Event>> getOrganizerEvents(
            @PathVariable UUID organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(organizerId, page, size));
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestBody Event event,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(event, UUID.fromString(userId)));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID eventId,
            @RequestBody Event updates,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, updates, UUID.fromString(userId)));
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<Event> publishEvent(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(eventService.publishEvent(eventId, UUID.fromString(userId)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
