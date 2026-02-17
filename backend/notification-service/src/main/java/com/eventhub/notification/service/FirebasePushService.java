package com.eventhub.notification.service;

import com.eventhub.notification.entity.DeviceToken;
import com.eventhub.notification.repository.DeviceTokenRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles Firebase Cloud Messaging (FCM) push notifications using the Firebase Admin SDK.
 * Manages device token registration/removal and sends push messages to user devices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirebasePushService {

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    // ─────────────────────────────────────────────
    // Device token management
    // ─────────────────────────────────────────────

    @Transactional
    public DeviceToken registerToken(UUID userId, String token, String deviceType, String deviceName) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByUserIdAndToken(userId, token);
        if (existing.isPresent()) {
            DeviceToken dt = existing.get();
            dt.setActive(true);
            dt.setDeviceType(deviceType != null ? deviceType : dt.getDeviceType());
            dt.setDeviceName(deviceName != null ? deviceName : dt.getDeviceName());
            log.debug("Re-activated existing FCM token for user={}", userId);
            return deviceTokenRepository.save(dt);
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .userId(userId)
                .token(token)
                .deviceType(deviceType != null ? deviceType : "WEB")
                .deviceName(deviceName)
                .active(true)
                .build();

        log.info("Registered new FCM token for user={} device={}", userId, deviceType);
        return deviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void removeToken(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
        log.info("Removed FCM token for user={}", userId);
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    }

    // ─────────────────────────────────────────────
    // Push notification sending
    // ─────────────────────────────────────────────

    /**
     * Send a push notification to all active devices of a user.
     */
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging not configured — skipping push for user={}", userId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("No active FCM tokens for user={}", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream().map(DeviceToken::getToken).toList();

        if (tokenStrings.size() == 1) {
            sendSingle(tokenStrings.get(0), title, body, data);
        } else {
            sendMulticast(tokenStrings, title, body, data);
        }
    }

    /**
     * Send to a single device token.
     */
    private void sendSingle(String token, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icons/eventhub-192.png")
                                    .setBadge("/icons/eventhub-badge.png")
                                    .build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setClickAction("OPEN_NOTIFICATION")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("FCM message sent: messageId={} token=...{}", messageId,
                    token.substring(Math.max(0, token.length() - 6)));

        } catch (FirebaseMessagingException e) {
            handleFcmError(token, e);
        }
    }

    /**
     * Send to multiple device tokens using multicast.
     */
    private void sendMulticast(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icons/eventhub-192.png")
                                    .build())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM multicast sent: success={} failure={} total={}",
                    response.getSuccessCount(), response.getFailureCount(), tokens.size());

            // Deactivate tokens that returned unregistered errors
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    FirebaseMessagingException ex = responses.get(i).getException();
                    if (ex != null && isTokenInvalid(ex)) {
                        deviceTokenRepository.deactivateToken(tokens.get(i));
                        log.warn("Deactivated stale FCM token: ...{}",
                                tokens.get(i).substring(Math.max(0, tokens.get(i).length() - 6)));
                    }
                }
            }

        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast failed: {}", e.getMessage(), e);
        }
    }

    private void handleFcmError(String token, FirebaseMessagingException e) {
        if (isTokenInvalid(e)) {
            deviceTokenRepository.deactivateToken(token);
            log.warn("Deactivated invalid FCM token: ...{} reason={}",
                    token.substring(Math.max(0, token.length() - 6)), e.getMessagingErrorCode());
        } else {
            log.error("FCM send failed: code={} message={}",
                    e.getMessagingErrorCode(), e.getMessage());
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
