package com.eventhub.event.service;

import com.eventhub.event.entity.Venue;
import com.eventhub.event.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public Page<Venue> getVenues(int page, int size, String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.isBlank()) {
            return venueRepository.searchVenues(search.trim(), pageable);
        }
        return venueRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Venue getVenue(UUID venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + venueId));
    }

    @Transactional(readOnly = true)
    public Page<Venue> getVenuesByOrganizer(UUID organizerId, int page, int size) {
        return venueRepository.findByCreatedBy(organizerId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public Venue createVenue(Venue venue, UUID organizerId) {
        venue.setCreatedBy(organizerId);
        venue = venueRepository.save(venue);
        log.info("Venue created: {} by organizer {}", venue.getId(), organizerId);
        return venue;
    }

    @Transactional
    public Venue updateVenue(UUID venueId, Venue updates, UUID organizerId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + venueId));

        if (!venue.getCreatedBy().equals(organizerId)) {
            throw new SecurityException("Not authorized to update this venue");
        }

        if (updates.getName() != null) venue.setName(updates.getName());
        if (updates.getAddress() != null) venue.setAddress(updates.getAddress());
        if (updates.getCity() != null) venue.setCity(updates.getCity());
        if (updates.getCountry() != null) venue.setCountry(updates.getCountry());
        if (updates.getLatitude() != null) venue.setLatitude(updates.getLatitude());
        if (updates.getLongitude() != null) venue.setLongitude(updates.getLongitude());
        if (updates.getCapacity() > 0) venue.setCapacity(updates.getCapacity());

        venue = venueRepository.save(venue);
        log.info("Venue updated: {}", venue.getId());
        return venue;
    }

    @Transactional
    public void deleteVenue(UUID venueId, UUID organizerId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + venueId));

        if (!venue.getCreatedBy().equals(organizerId)) {
            throw new SecurityException("Not authorized to delete this venue");
        }

        venueRepository.delete(venue);
        log.info("Venue deleted: {}", venueId);
    }
}
