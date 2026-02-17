package com.eventhub.event.controller;

import com.eventhub.event.entity.VenueLayout;
import com.eventhub.event.service.VenueLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueLayoutController {

    private final VenueLayoutService layoutService;

    @GetMapping("/{venueId}/layouts")
    public ResponseEntity<List<VenueLayout>> getLayoutsForVenue(@PathVariable UUID venueId) {
        return ResponseEntity.ok(layoutService.getLayoutsForVenue(venueId));
    }

    @GetMapping("/layouts/{layoutId}")
    public ResponseEntity<VenueLayout> getLayout(@PathVariable UUID layoutId) {
        return ResponseEntity.ok(layoutService.getLayout(layoutId));
    }

    @GetMapping("/layouts/templates")
    public ResponseEntity<List<VenueLayout>> getTemplates(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(layoutService.getTemplatesByType(type));
        }
        return ResponseEntity.ok(layoutService.getTemplates());
    }

    @PostMapping("/{venueId}/layouts")
    public ResponseEntity<VenueLayout> createLayout(
            @PathVariable UUID venueId,
            @RequestBody VenueLayout layout,
            @RequestHeader("X-User-Id") String userId) {
        layout.setVenueId(venueId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(layoutService.createLayout(layout, UUID.fromString(userId)));
    }

    @PutMapping("/layouts/{layoutId}")
    public ResponseEntity<VenueLayout> updateLayout(
            @PathVariable UUID layoutId,
            @RequestBody VenueLayout updates,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(layoutService.updateLayout(layoutId, updates, UUID.fromString(userId)));
    }

    @DeleteMapping("/layouts/{layoutId}")
    public ResponseEntity<Void> deleteLayout(
            @PathVariable UUID layoutId,
            @RequestHeader("X-User-Id") String userId) {
        layoutService.deleteLayout(layoutId, UUID.fromString(userId));
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
