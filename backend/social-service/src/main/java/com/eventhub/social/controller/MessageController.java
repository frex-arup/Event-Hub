package com.eventhub.social.controller;

import com.eventhub.social.entity.DirectMessage;
import com.eventhub.social.entity.EventDiscussion;
import com.eventhub.social.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @PostMapping
    public ResponseEntity<DirectMessage> sendMessage(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messagingService.sendMessage(
                        UUID.fromString(userId),
                        UUID.fromString(body.get("receiverId")),
                        body.get("content")));
    }

    @GetMapping("/conversation/{partnerId}")
    public ResponseEntity<Page<DirectMessage>> getConversation(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messagingService.getConversation(
                UUID.fromString(userId), partnerId, page, size));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<UUID>> getConversationPartners(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(messagingService.getConversationPartners(UUID.fromString(userId)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(Map.of("count", messagingService.getUnreadCount(UUID.fromString(userId))));
    }

    @PostMapping("/conversation/{partnerId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID partnerId) {
        messagingService.markConversationAsRead(UUID.fromString(userId), partnerId);
        return ResponseEntity.ok().build();
    }

    // ─── Event Discussions ───

    @GetMapping("/discussions/{eventId}")
    public ResponseEntity<Page<EventDiscussion>> getEventDiscussions(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messagingService.getEventDiscussions(eventId, page, size));
    }

    @PostMapping("/discussions/{eventId}")
    public ResponseEntity<EventDiscussion> addDiscussionMessage(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messagingService.addDiscussionMessage(
                        eventId, UUID.fromString(userId), body.get("content")));
    }
}
