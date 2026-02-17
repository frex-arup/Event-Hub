package com.eventhub.event.service;

import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventCategory;
import com.eventhub.event.entity.EventStatus;
import com.eventhub.event.entity.Venue;
import com.eventhub.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class EventService {

    private final EventRepository eventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public Page<Event> getEvents(int page, int size, String category, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));

        if (search != null && !search.isBlank()) {
            return eventRepository.searchEvents(search.trim(), pageable);
        }

        if (category != null && !category.isBlank()) {
            try {
                EventCategory cat = EventCategory.valueOf(category.toUpperCase());
                return eventRepository.findByStatusAndCategory(EventStatus.PUBLISHED, cat, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category: {}", category);
            }
        }

        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    }

    @Transactional(readOnly = true)
    public List<Event> getTrendingEvents() {
        return eventRepository.findTrendingEvents(PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public Page<Event> getOrganizerEvents(UUID organizerId, int page, int size) {
        return eventRepository.findByOrganizerId(organizerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public Event createEvent(Event event, UUID organizerId) {
        event.setOrganizerId(organizerId);
        event.setStatus(EventStatus.DRAFT);
        event = eventRepository.save(event);

        log.info("Event created: {} by organizer {}", event.getId(), organizerId);

        publishEventMessage("event.created", event);
        return event;
    }

    @Transactional
    public Event updateEvent(UUID eventId, Event updates, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized to update this event");
        }

        if (updates.getTitle() != null) event.setTitle(updates.getTitle());
        if (updates.getDescription() != null) event.setDescription(updates.getDescription());
        if (updates.getCategory() != null) event.setCategory(updates.getCategory());
        if (updates.getCoverImageUrl() != null) event.setCoverImageUrl(updates.getCoverImageUrl());
        if (updates.getStartDate() != null) event.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) event.setEndDate(updates.getEndDate());
        if (updates.getTags() != null && !updates.getTags().isEmpty()) event.setTags(updates.getTags());

        event = eventRepository.save(event);
        publishEventMessage("event.updated", event);
        return event;
    }

    @Transactional
    public Event publishEvent(UUID eventId, UUID organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        log.info("Event published: {}", event.getId());
        publishEventMessage("event.published", event);
        return event;
    }

    @Transactional
    public void updateAvailableSeats(UUID eventId, int delta) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        int newAvailable = event.getAvailableSeats() + delta;
        if (newAvailable < 0) newAvailable = 0;
        event.setAvailableSeats(newAvailable);
        eventRepository.save(event);
    }

    private void publishEventMessage(String eventType, Event event) {
        try {
            kafkaTemplate.send("event-events", event.getId().toString(), Map.of(
                    "eventType", eventType,
                    "eventId", event.getId().toString(),
                    "title", event.getTitle(),
                    "status", event.getStatus().name(),
                    "organizerId", event.getOrganizerId().toString(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish event message: {}", e.getMessage());
        }
    }
}
