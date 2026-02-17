package com.eventhub.event.service;

import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventSession;
import com.eventhub.event.repository.EventRepository;
import com.eventhub.event.repository.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSessionService {

    private final EventSessionRepository sessionRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventSession> getSessionsForEvent(UUID eventId) {
        return sessionRepository.findByEventIdOrderByStartTimeAsc(eventId);
    }

    @Transactional(readOnly = true)
    public EventSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    @Transactional
    public EventSession createSession(EventSession session, UUID organizerId) {
        Event event = eventRepository.findById(session.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + session.getEventId()));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized to add sessions to this event");
        }

        session = sessionRepository.save(session);
        log.info("Session created: {} for event {}", session.getId(), session.getEventId());
        return session;
    }

    @Transactional
    public EventSession updateSession(UUID sessionId, EventSession updates, UUID organizerId) {
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        Event event = eventRepository.findById(session.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized to update this session");
        }

        if (updates.getTitle() != null) session.setTitle(updates.getTitle());
        if (updates.getDescription() != null) session.setDescription(updates.getDescription());
        if (updates.getSpeaker() != null) session.setSpeaker(updates.getSpeaker());
        if (updates.getStartTime() != null) session.setStartTime(updates.getStartTime());
        if (updates.getEndTime() != null) session.setEndTime(updates.getEndTime());
        if (updates.getRoom() != null) session.setRoom(updates.getRoom());

        session = sessionRepository.save(session);
        log.info("Session updated: {}", session.getId());
        return session;
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID organizerId) {
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        Event event = eventRepository.findById(session.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized to delete this session");
        }

        sessionRepository.delete(session);
        log.info("Session deleted: {}", sessionId);
    }
}
