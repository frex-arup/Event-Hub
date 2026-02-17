package com.eventhub.event.service;

import com.eventhub.event.entity.*;
import com.eventhub.event.repository.*;
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
public class EventActivityService {

    private final EventPollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollVoteRepository voteRepository;
    private final EventAnnouncementRepository announcementRepository;
    private final SessionCheckinRepository checkinRepository;
    private final EventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─────────────────────────────────────────────
    // Polls
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventPoll> getPolls(UUID eventId, int page, int size) {
        return pollRepository.findByEventIdOrderByCreatedAtDesc(eventId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<EventPoll> getActivePolls(UUID eventId) {
        return pollRepository.findByEventIdAndIsActiveTrue(eventId);
    }

    @Transactional
    public EventPoll createPoll(UUID eventId, UUID authorId, String question,
                                 List<String> optionLabels, Instant endsAt) {
        verifyOrganizer(eventId, authorId);

        EventPoll poll = EventPoll.builder()
                .eventId(eventId)
                .authorId(authorId)
                .question(question)
                .endsAt(endsAt)
                .build();
        poll = pollRepository.save(poll);

        for (int i = 0; i < optionLabels.size(); i++) {
            PollOption option = PollOption.builder()
                    .pollId(poll.getId())
                    .label(optionLabels.get(i))
                    .sortOrder(i)
                    .build();
            optionRepository.save(option);
        }

        log.info("Poll created: {} for event {}", poll.getId(), eventId);
        return pollRepository.findById(poll.getId()).orElse(poll);
    }

    @Transactional
    public void vote(UUID pollId, UUID userId, UUID optionId) {
        EventPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));

        if (!poll.isActive()) {
            throw new IllegalStateException("Poll is no longer active");
        }
        if (poll.getEndsAt() != null && Instant.now().isAfter(poll.getEndsAt())) {
            throw new IllegalStateException("Poll has ended");
        }

        // Check if already voted — if so, change vote
        voteRepository.findByPollIdAndUserId(pollId, userId).ifPresent(existing -> {
            optionRepository.decrementVoteCount(existing.getOptionId());
            voteRepository.delete(existing);
        });

        PollVote vote = PollVote.builder()
                .pollId(pollId)
                .userId(userId)
                .optionId(optionId)
                .build();
        voteRepository.save(vote);
        optionRepository.incrementVoteCount(optionId);

        log.info("Vote recorded: poll={} user={} option={}", pollId, userId, optionId);
    }

    @Transactional
    public void closePoll(UUID pollId, UUID authorId) {
        EventPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));
        if (!poll.getAuthorId().equals(authorId)) {
            throw new SecurityException("Not authorized to close this poll");
        }
        poll.setActive(false);
        pollRepository.save(poll);
        log.info("Poll closed: {}", pollId);
    }

    // ─────────────────────────────────────────────
    // Announcements
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventAnnouncement> getAnnouncements(UUID eventId, int page, int size) {
        return announcementRepository.findByEventIdOrderByCreatedAtDesc(eventId, PageRequest.of(page, size));
    }

    @Transactional
    public EventAnnouncement createAnnouncement(UUID eventId, UUID authorId,
                                                  String title, String content, String priority) {
        verifyOrganizer(eventId, authorId);

        EventAnnouncement announcement = EventAnnouncement.builder()
                .eventId(eventId)
                .authorId(authorId)
                .title(title)
                .content(content)
                .priority(priority != null ? priority : "NORMAL")
                .build();
        announcement = announcementRepository.save(announcement);

        // Notify attendees via Kafka
        try {
            kafkaTemplate.send("notification-events", eventId.toString(), Map.of(
                    "eventType", "event.announcement",
                    "eventId", eventId.toString(),
                    "title", title,
                    "priority", announcement.getPriority(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish announcement notification: {}", e.getMessage());
        }

        log.info("Announcement created: {} for event {}", announcement.getId(), eventId);
        return announcement;
    }

    // ─────────────────────────────────────────────
    // Session Check-ins
    // ─────────────────────────────────────────────

    @Transactional
    public SessionCheckin checkin(UUID sessionId, UUID userId) {
        if (checkinRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new IllegalStateException("Already checked in to this session");
        }

        SessionCheckin checkin = SessionCheckin.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();
        checkin = checkinRepository.save(checkin);
        log.info("Check-in: user {} → session {}", userId, sessionId);
        return checkin;
    }

    @Transactional(readOnly = true)
    public List<SessionCheckin> getSessionCheckins(UUID sessionId) {
        return checkinRepository.findBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public long getCheckinCount(UUID sessionId) {
        return checkinRepository.countBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedIn(UUID sessionId, UUID userId) {
        return checkinRepository.existsBySessionIdAndUserId(sessionId, userId);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private void verifyOrganizer(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (!event.getOrganizerId().equals(userId)) {
            throw new SecurityException("Not authorized for this event");
        }
    }
}
