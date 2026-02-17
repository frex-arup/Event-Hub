package com.eventhub.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venue_layouts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_json", nullable = false, columnDefinition = "jsonb")
    private String layoutJson;

    @Column(name = "canvas_width", nullable = false)
    @Builder.Default
    private int canvasWidth = 800;

    @Column(name = "canvas_height", nullable = false)
    @Builder.Default
    private int canvasHeight = 600;

    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private boolean isTemplate = false;

    @Column(name = "template_type")
    private String templateType;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
