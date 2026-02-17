package com.eventhub.event.repository;

import com.eventhub.event.entity.VenueLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueLayoutRepository extends JpaRepository<VenueLayout, UUID> {

    List<VenueLayout> findByVenueId(UUID venueId);

    List<VenueLayout> findByIsTemplateTrue();

    List<VenueLayout> findByVenueIdAndIsTemplateFalse(UUID venueId);

    List<VenueLayout> findByTemplateType(String templateType);
}
