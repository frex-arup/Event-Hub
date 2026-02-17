package com.eventhub.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "layout_id")
    private UUID layoutId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "total_seats", nullable = false)
    @Builder.Default
    private int totalSeats = 0;

    @Column(name = "available_seats", nullable = false)
    @Builder.Default
    private int availableSeats = 0;

    @Column(name = "min_price", nullable = false)
    @Builder.Default
    private BigDecimal minPrice = BigDecimal.ZERO;

    @Column(name = "max_price", nullable = false)
    @Builder.Default
    private BigDecimal maxPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
