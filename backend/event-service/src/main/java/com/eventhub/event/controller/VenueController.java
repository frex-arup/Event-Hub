package com.eventhub.event.controller;

import com.eventhub.event.entity.Venue;
import com.eventhub.event.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<Page<Venue>> getVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(venueService.getVenues(page, size, search));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<Venue> getVenue(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getVenue(venueId));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<Page<Venue>> getOrganizerVenues(
            @PathVariable UUID organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(venueService.getVenuesByOrganizer(organizerId, page, size));
    }

    @PostMapping
    public ResponseEntity<Venue> createVenue(
            @RequestBody Venue venue,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createVenue(venue, UUID.fromString(userId)));
    }

    @PutMapping("/{venueId}")
    public ResponseEntity<Venue> updateVenue(
            @PathVariable UUID venueId,
            @RequestBody Venue updates,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(venueService.updateVenue(venueId, updates, UUID.fromString(userId)));
    }

    @DeleteMapping("/{venueId}")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable UUID venueId,
            @RequestHeader("X-User-Id") String userId) {
        venueService.deleteVenue(venueId, UUID.fromString(userId));
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
