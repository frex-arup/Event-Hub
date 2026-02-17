package com.eventhub.event.service;

import com.eventhub.event.entity.Venue;
import com.eventhub.event.entity.VenueLayout;
import com.eventhub.event.repository.VenueLayoutRepository;
import com.eventhub.event.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueLayoutService {

    private final VenueLayoutRepository layoutRepository;
    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<VenueLayout> getLayoutsForVenue(UUID venueId) {
        return layoutRepository.findByVenueIdAndIsTemplateFalse(venueId);
    }

    @Transactional(readOnly = true)
    public VenueLayout getLayout(UUID layoutId) {
        return layoutRepository.findById(layoutId)
                .orElseThrow(() -> new IllegalArgumentException("Layout not found: " + layoutId));
    }

    @Transactional(readOnly = true)
    public List<VenueLayout> getTemplates() {
        return layoutRepository.findByIsTemplateTrue();
    }

    @Transactional(readOnly = true)
    public List<VenueLayout> getTemplatesByType(String templateType) {
        return layoutRepository.findByTemplateType(templateType);
    }

    @Transactional
    public VenueLayout createLayout(VenueLayout layout, UUID organizerId) {
        // Verify venue exists and belongs to organizer
        Venue venue = venueRepository.findById(layout.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + layout.getVenueId()));

        if (!venue.getCreatedBy().equals(organizerId)) {
            throw new SecurityException("Not authorized to create layouts for this venue");
        }

        layout = layoutRepository.save(layout);
        log.info("Layout created: {} for venue {} by organizer {}", layout.getId(), layout.getVenueId(), organizerId);
        return layout;
    }

    @Transactional
    public VenueLayout updateLayout(UUID layoutId, VenueLayout updates, UUID organizerId) {
        VenueLayout layout = layoutRepository.findById(layoutId)
                .orElseThrow(() -> new IllegalArgumentException("Layout not found: " + layoutId));

        Venue venue = venueRepository.findById(layout.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (!venue.getCreatedBy().equals(organizerId)) {
            throw new SecurityException("Not authorized to update this layout");
        }

        if (updates.getName() != null) layout.setName(updates.getName());
        if (updates.getLayoutJson() != null) layout.setLayoutJson(updates.getLayoutJson());
        if (updates.getCanvasWidth() > 0) layout.setCanvasWidth(updates.getCanvasWidth());
        if (updates.getCanvasHeight() > 0) layout.setCanvasHeight(updates.getCanvasHeight());

        layout = layoutRepository.save(layout);
        log.info("Layout updated: {}", layout.getId());
        return layout;
    }

    @Transactional
    public void deleteLayout(UUID layoutId, UUID organizerId) {
        VenueLayout layout = layoutRepository.findById(layoutId)
                .orElseThrow(() -> new IllegalArgumentException("Layout not found: " + layoutId));

        Venue venue = venueRepository.findById(layout.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (!venue.getCreatedBy().equals(organizerId)) {
            throw new SecurityException("Not authorized to delete this layout");
        }

        layoutRepository.delete(layout);
        log.info("Layout deleted: {}", layoutId);
    }

    @Transactional
    public VenueLayout createTemplate(VenueLayout layout) {
        layout.setTemplate(true);
        layout = layoutRepository.save(layout);
        log.info("Template created: {} type={}", layout.getId(), layout.getTemplateType());
        return layout;
    }
}
