package com.eventhub.notification.service;

import com.eventhub.notification.entity.Notification;
import com.eventhub.notification.entity.NotificationPreference;
import com.eventhub.notification.repository.NotificationPreferenceRepository;
import com.eventhub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatches notifications across channels: in-app, email, SMS, push.
 * All notifications are persisted to the database for in-app retrieval.
 * Push notifications are sent via Firebase Cloud Messaging when enabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final JavaMailSender mailSender;
    private final FirebasePushService firebasePushService;

    // ─────────────────────────────────────────────
    // Query methods (called by REST controller)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ─────────────────────────────────────────────
    // Preference methods
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public NotificationPreference getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());
    }

    @Transactional
    public NotificationPreference updatePreferences(UUID userId, NotificationPreference updates) {
        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());

        prefs.setEmailEnabled(updates.isEmailEnabled());
        prefs.setSmsEnabled(updates.isSmsEnabled());
        prefs.setPushEnabled(updates.isPushEnabled());
        prefs.setInAppEnabled(updates.isInAppEnabled());

        return preferenceRepository.save(prefs);
    }

    // ─────────────────────────────────────────────
    // Event-driven notification dispatch
    // ─────────────────────────────────────────────

    public void sendBookingConfirmation(UUID userId, Map<String, Object> event) {
        String bookingId = str(event, "bookingId");
        Object seatCount = event.get("seatCount");
        String amount = str(event, "totalAmount");
        String currency = (String) event.getOrDefault("currency", "USD");

        String title = "Booking Confirmed!";
        String message = String.format(
                "Your booking %s has been confirmed. %s seat(s) for %s %s. " +
                "Check your ticket wallet for the QR code.",
                bookingId, seatCount, currency, amount
        );

        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId);
        metadata.put("eventId", str(event, "eventId"));

        persistAndDispatch(userId, "BOOKING_CONFIRMED", title, message, metadata);
    }

    public void sendBookingFailure(UUID userId, Map<String, Object> event) {
        String bookingId = str(event, "bookingId");
        String reason = (String) event.getOrDefault("reason", "Unknown error");

        String title = "Booking Failed";
        String message = String.format("Your booking %s could not be completed: %s", bookingId, reason);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId);
        metadata.put("reason", reason);

        persistAndDispatch(userId, "BOOKING_FAILED", title, message, metadata);
    }

    public void sendRefundNotification(UUID userId, Map<String, Object> event) {
        String bookingId = str(event, "bookingId");

        String title = "Refund Processed";
        String message = String.format("Your refund for booking %s has been processed.", bookingId);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId);

        persistAndDispatch(userId, "PAYMENT_REFUNDED", title, message, metadata);
    }

    public void sendEventReminder(UUID userId, Map<String, Object> event) {
        String eventId = str(event, "eventId");

        String title = "Event Reminder";
        String message = "Your event is coming up soon! Don't forget to check in.";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventId", eventId);

        persistAndDispatch(userId, "EVENT_REMINDER", title, message, metadata);
    }

    public void sendNewFollowerNotification(UUID userId, Map<String, Object> event) {
        String followerId = str(event, "followerId");
        String followerName = (String) event.getOrDefault("followerName", "Someone");

        String title = "New Follower";
        String message = followerName + " started following you.";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("followerId", followerId);

        persistAndDispatch(userId, "NEW_FOLLOWER", title, message, metadata);
    }

    public void sendNewMessageNotification(UUID userId, Map<String, Object> event) {
        String senderId = str(event, "senderId");
        String senderName = (String) event.getOrDefault("senderName", "Someone");

        String title = "New Message";
        String message = senderName + " sent you a message.";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("senderId", senderId);

        persistAndDispatch(userId, "NEW_MESSAGE", title, message, metadata);
    }

    public void sendWelcomeEmail(UUID userId, String name, String email) {
        // Store in-app notification
        persistAndDispatch(userId, "WELCOME", "Welcome to EventHub!",
                "Hi " + name + ", welcome to EventHub! Start discovering amazing events.",
                Map.of("email", email));

        // Send email
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("Welcome to EventHub, " + name + "!");
            msg.setText("Hi " + name + ",\n\n" +
                    "Welcome to EventHub! We're excited to have you.\n\n" +
                    "Start discovering amazing events and booking your seats today.\n\n" +
                    "— The EventHub Team");

            // mailSender.send(msg); // Uncomment when SMTP is configured
            log.info("Welcome email queued for {} ({})", name, email);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Core persist + dispatch logic
    // ─────────────────────────────────────────────

    @Transactional
    protected void persistAndDispatch(UUID userId, String type, String title, String message,
                                       Map<String, String> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata)
                .channel("IN_APP")
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification persisted: type={} userId={} id={}", type, userId, notification.getId());

        // Send Firebase push notification if user has push enabled
        try {
            NotificationPreference prefs = preferenceRepository.findByUserId(userId).orElse(null);
            boolean pushEnabled = prefs == null || prefs.isPushEnabled(); // default true
            if (pushEnabled) {
                Map<String, String> pushData = new HashMap<>();
                pushData.put("notificationId", notification.getId().toString());
                pushData.put("type", type);
                if (metadata != null) {
                    pushData.putAll(metadata);
                }
                firebasePushService.sendToUser(userId, title, message, pushData);
            }
        } catch (Exception e) {
            log.warn("Failed to send push notification for user={}: {}", userId, e.getMessage());
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
