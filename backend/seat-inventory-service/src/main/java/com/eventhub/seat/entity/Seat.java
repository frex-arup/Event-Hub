package com.eventhub.seat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "section_id", nullable = false)
    private String sectionId;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "x_pos")
    private double xPos;

    @Column(name = "y_pos")
    private double yPos;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "booked_by")
    private UUID bookedBy;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Version
    private int version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isLockExpired() {
        return lockExpiresAt != null && Instant.now().isAfter(lockExpiresAt);
    }
}
