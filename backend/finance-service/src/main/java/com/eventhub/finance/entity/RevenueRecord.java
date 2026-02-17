package com.eventhub.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revenue_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "TICKET_SALE";

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
