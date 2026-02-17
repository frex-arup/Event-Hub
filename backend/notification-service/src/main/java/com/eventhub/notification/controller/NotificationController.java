package com.eventhub.notification.controller;

import com.eventhub.notification.entity.DeviceToken;
import com.eventhub.notification.entity.Notification;
import com.eventhub.notification.entity.NotificationPreference;
import com.eventhub.notification.service.FirebasePushService;
import com.eventhub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final FirebasePushService firebasePushService;

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(UUID.fromString(userId), page, size));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<Notification>> getUnreadNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(UUID.fromString(userId), page, size));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") String userId) {
        long count = notificationService.getUnreadCount(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAsRead(notificationId, UUID.fromString(userId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") String userId) {
        notificationService.markAllAsRead(UUID.fromString(userId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreference> getPreferences(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(notificationService.getPreferences(UUID.fromString(userId)));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody NotificationPreference preferences) {
        return ResponseEntity.ok(notificationService.updatePreferences(UUID.fromString(userId), preferences));
    }

    // ─────────────────────────────────────────────
    // FCM Device Token endpoints
    // ─────────────────────────────────────────────

    @PostMapping("/devices")
    public ResponseEntity<DeviceToken> registerDeviceToken(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String deviceType = body.getOrDefault("deviceType", "WEB");
        String deviceName = body.get("deviceName");
        DeviceToken saved = firebasePushService.registerToken(
                UUID.fromString(userId), token, deviceType, deviceName);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/devices")
    public ResponseEntity<Void> removeDeviceToken(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        firebasePushService.removeToken(UUID.fromString(userId), token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceToken>> getDeviceTokens(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(firebasePushService.getActiveTokens(UUID.fromString(userId)));
    }
}
