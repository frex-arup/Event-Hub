package com.eventhub.social.service;

import com.eventhub.social.entity.DirectMessage;
import com.eventhub.social.entity.EventDiscussion;
import com.eventhub.social.repository.DirectMessageRepository;
import com.eventhub.social.repository.EventDiscussionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final DirectMessageRepository dmRepository;
    private final EventDiscussionRepository discussionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Direct Messages ───

    @Transactional
    public DirectMessage sendMessage(UUID senderId, UUID receiverId, String content) {
        DirectMessage dm = DirectMessage.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .build();
        dm = dmRepository.save(dm);
        log.info("DM sent: {} → {} (id={})", senderId, receiverId, dm.getId());

        // Notify receiver
        try {
            kafkaTemplate.send("notification-events", receiverId.toString(), Map.of(
                    "eventType", "new.message",
                    "userId", receiverId.toString(),
                    "senderId", senderId.toString(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to send DM notification: {}", e.getMessage());
        }

        return dm;
    }

    @Transactional(readOnly = true)
    public Page<DirectMessage> getConversation(UUID userId1, UUID userId2, int page, int size) {
        return dmRepository.findConversation(userId1, userId2, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<UUID> getConversationPartners(UUID userId) {
        return dmRepository.findConversationPartners(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return dmRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public void markConversationAsRead(UUID receiverId, UUID senderId) {
        dmRepository.markConversationAsRead(receiverId, senderId);
    }

    // ─── Event Discussions ───

    @Transactional
    public EventDiscussion addDiscussionMessage(UUID eventId, UUID authorId, String content) {
        EventDiscussion discussion = EventDiscussion.builder()
                .eventId(eventId)
                .authorId(authorId)
                .content(content)
                .build();
        discussion = discussionRepository.save(discussion);
        log.info("Discussion message added for event {} by user {}", eventId, authorId);
        return discussion;
    }

    @Transactional(readOnly = true)
    public Page<EventDiscussion> getEventDiscussions(UUID eventId, int page, int size) {
        return discussionRepository.findByEventIdOrderByCreatedAtDesc(eventId, PageRequest.of(page, size));
    }
}
