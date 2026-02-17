package com.eventhub.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_follows")
@IdClass(UserFollow.UserFollowId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFollow {

    @Id
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Id
    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFollowId implements Serializable {
        private UUID followerId;
        private UUID followingId;
    }
}
