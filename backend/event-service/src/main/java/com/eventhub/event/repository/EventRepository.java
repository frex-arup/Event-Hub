package com.eventhub.event.repository;

import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventCategory;
import com.eventhub.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByStatusAndCategory(EventStatus status, EventCategory category, Pageable pageable);

    Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchEvents(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' ORDER BY e.availableSeats DESC")
    List<Event> findTrendingEvents(Pageable pageable);
}
