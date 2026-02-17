package com.eventhub.event.repository;

import com.eventhub.event.entity.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    Page<Venue> findByCreatedBy(UUID createdBy, Pageable pageable);

    @Query("SELECT v FROM Venue v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Venue> searchVenues(@Param("query") String query, Pageable pageable);

    List<Venue> findByCity(String city);
}
