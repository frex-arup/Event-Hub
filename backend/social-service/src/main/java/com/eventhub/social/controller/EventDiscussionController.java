package com.eventhub.social.controller;

import com.eventhub.social.entity.EventDiscussion;
import com.eventhub.social.service.EventDiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/discussions")
@RequiredArgsConstructor
public class EventDiscussionController {

    private final EventDiscussionService discussionService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Page<EventDiscussion>> getDiscussions(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(discussionService.getDiscussions(eventId, page, size));
    }

    @GetMapping("/{discussionId}")
    public ResponseEntity<EventDiscussion> getDiscussion(@PathVariable UUID discussionId) {
        return ResponseEntity.ok(discussionService.getDiscussion(discussionId));
    }

    @PostMapping("/event/{eventId}")
    public ResponseEntity<EventDiscussion> createDiscussion(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.createDiscussion(
                        eventId, UUID.fromString(userId), body.get("content")));
    }

    @DeleteMapping("/{discussionId}")
    public ResponseEntity<Void> deleteDiscussion(
            @PathVariable UUID discussionId,
            @RequestHeader("X-User-Id") String userId) {
        discussionService.deleteDiscussion(discussionId, UUID.fromString(userId));
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
