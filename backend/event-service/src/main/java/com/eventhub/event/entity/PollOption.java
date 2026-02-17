package com.eventhub.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "poll_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(nullable = false)
    private String label;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private int voteCount = 0;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
