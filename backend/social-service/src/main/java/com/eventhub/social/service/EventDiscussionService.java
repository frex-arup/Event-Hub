package com.eventhub.social.service;

import com.eventhub.social.entity.EventDiscussion;
import com.eventhub.social.repository.EventDiscussionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventDiscussionService {

    private final EventDiscussionRepository discussionRepository;

    @Transactional(readOnly = true)
    public Page<EventDiscussion> getDiscussions(UUID eventId, int page, int size) {
        return discussionRepository.findByEventIdOrderByCreatedAtDesc(eventId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public EventDiscussion getDiscussion(UUID discussionId) {
        return discussionRepository.findById(discussionId)
                .orElseThrow(() -> new IllegalArgumentException("Discussion not found: " + discussionId));
    }

    @Transactional
    public EventDiscussion createDiscussion(UUID eventId, UUID authorId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Discussion content cannot be empty");
        }

        EventDiscussion discussion = EventDiscussion.builder()
                .eventId(eventId)
                .authorId(authorId)
                .content(content.trim())
                .build();

        discussion = discussionRepository.save(discussion);
        log.info("Discussion created: {} for event {} by user {}", discussion.getId(), eventId, authorId);
        return discussion;
    }

    @Transactional
    public void deleteDiscussion(UUID discussionId, UUID userId) {
        EventDiscussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new IllegalArgumentException("Discussion not found: " + discussionId));

        if (!discussion.getAuthorId().equals(userId)) {
            throw new SecurityException("Not authorized to delete this discussion");
        }

        discussionRepository.delete(discussion);
        log.info("Discussion deleted: {}", discussionId);
    }
}
