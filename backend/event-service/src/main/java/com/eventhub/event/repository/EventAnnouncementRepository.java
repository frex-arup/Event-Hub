package com.eventhub.event.repository;

import com.eventhub.event.entity.EventAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventAnnouncementRepository extends JpaRepository<EventAnnouncement, UUID> {

    Page<EventAnnouncement> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
}
