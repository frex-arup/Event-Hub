package com.eventhub.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "device_type", length = 20)
    @Builder.Default
    private String deviceType = "WEB";

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
