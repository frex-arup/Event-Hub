package com.eventhub.event.repository;

import com.eventhub.event.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, UUID> {

    List<EventSession> findByEventIdOrderByStartTimeAsc(UUID eventId);

    void deleteByEventId(UUID eventId);
}
