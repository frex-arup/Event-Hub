package com.eventhub.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_amount", nullable = false)
    @Builder.Default
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount")
    private BigDecimal actualAmount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
