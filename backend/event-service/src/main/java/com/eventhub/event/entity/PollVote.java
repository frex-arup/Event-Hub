package com.eventhub.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_votes")
@IdClass(PollVote.PollVoteId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PollVote {

    @Id
    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PollVoteId implements Serializable {
        private UUID pollId;
        private UUID userId;
    }
}
