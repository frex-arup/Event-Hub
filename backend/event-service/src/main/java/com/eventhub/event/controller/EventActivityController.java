package com.eventhub.event.controller;

import com.eventhub.event.entity.EventAnnouncement;
import com.eventhub.event.entity.EventPoll;
import com.eventhub.event.entity.SessionCheckin;
import com.eventhub.event.service.EventActivityService;
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
public class EventActivityController {

    private final EventActivityService activityService;

    // ─── Polls ───

    @GetMapping("/{eventId}/polls")
    public ResponseEntity<Page<EventPoll>> getPolls(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.getPolls(eventId, page, size));
    }

    @GetMapping("/{eventId}/polls/active")
    public ResponseEntity<List<EventPoll>> getActivePolls(@PathVariable UUID eventId) {
        return ResponseEntity.ok(activityService.getActivePolls(eventId));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/{eventId}/polls")
    public ResponseEntity<EventPoll> createPoll(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        List<String> options = (List<String>) body.get("options");
        String endsAtStr = (String) body.get("endsAt");
        Instant endsAt = endsAtStr != null ? Instant.parse(endsAtStr) : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createPoll(eventId, UUID.fromString(userId), question, options, endsAt));
    }

    @PostMapping("/polls/{pollId}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable UUID pollId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        activityService.vote(pollId, UUID.fromString(userId), UUID.fromString(body.get("optionId")));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/polls/{pollId}/close")
    public ResponseEntity<Void> closePoll(
            @PathVariable UUID pollId,
            @RequestHeader("X-User-Id") String userId) {
        activityService.closePoll(pollId, UUID.fromString(userId));
        return ResponseEntity.ok().build();
    }

    // ─── Announcements ───

    @GetMapping("/{eventId}/announcements")
    public ResponseEntity<Page<EventAnnouncement>> getAnnouncements(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityService.getAnnouncements(eventId, page, size));
    }

    @PostMapping("/{eventId}/announcements")
    public ResponseEntity<EventAnnouncement> createAnnouncement(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createAnnouncement(
                        eventId, UUID.fromString(userId),
                        body.get("title"), body.get("content"), body.get("priority")));
    }

    // ─── Session Check-ins ───

    @PostMapping("/sessions/{sessionId}/checkin")
    public ResponseEntity<SessionCheckin> checkin(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.checkin(sessionId, UUID.fromString(userId)));
    }

    @GetMapping("/sessions/{sessionId}/checkins")
    public ResponseEntity<List<SessionCheckin>> getCheckins(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(activityService.getSessionCheckins(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/checkin-count")
    public ResponseEntity<Map<String, Long>> getCheckinCount(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(Map.of("count", activityService.getCheckinCount(sessionId)));
    }

    @GetMapping("/sessions/{sessionId}/checked-in")
    public ResponseEntity<Map<String, Boolean>> isCheckedIn(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(Map.of("checkedIn",
                activityService.isCheckedIn(sessionId, UUID.fromString(userId))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler({SecurityException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException e) {
        int status = e instanceof SecurityException ? 403 : 400;
        return ResponseEntity.status(status).body(Map.of(
                "status", status, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }
}
