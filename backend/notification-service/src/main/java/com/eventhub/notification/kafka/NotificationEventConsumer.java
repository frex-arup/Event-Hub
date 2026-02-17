package com.eventhub.notification.kafka;

import com.eventhub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes notification events from all services and dispatches
 * to the appropriate notification channels (email, SMS, push, in-app).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void handleNotificationEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String userIdStr = (String) event.get("userId");

        if (eventType == null || userIdStr == null) {
            log.warn("Invalid notification event: {}", event);
            return;
        }

        UUID userId = UUID.fromString(userIdStr);

        try {
            switch (eventType) {
                case "booking.confirmed" -> notificationService.sendBookingConfirmation(userId, event);
                case "booking.failed" -> notificationService.sendBookingFailure(userId, event);
                case "payment.refunded" -> notificationService.sendRefundNotification(userId, event);
                case "event.reminder" -> notificationService.sendEventReminder(userId, event);
                case "new.follower" -> notificationService.sendNewFollowerNotification(userId, event);
                case "new.message" -> notificationService.sendNewMessageNotification(userId, event);
                default -> log.debug("Unhandled notification event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing notification event {} for user {}: {}",
                    eventType, userId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void handleUserEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");

        try {
            if ("user.registered".equals(eventType)) {
                String userId = (String) event.get("userId");
                String email = (String) event.get("email");
                String name = (String) event.get("name");
                log.info("Sending welcome email to {} ({})", name, email);
                notificationService.sendWelcomeEmail(UUID.fromString(userId), name, email);
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
        }
    }
}
