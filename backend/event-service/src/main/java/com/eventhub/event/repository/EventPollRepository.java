package com.eventhub.event.repository;

import com.eventhub.event.entity.EventPoll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventPollRepository extends JpaRepository<EventPoll, UUID> {

    Page<EventPoll> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);

    List<EventPoll> findByEventIdAndIsActiveTrue(UUID eventId);
}
