package com.eventhub.event.repository;

import com.eventhub.event.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, PollVote.PollVoteId> {

    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);

    long countByPollId(UUID pollId);
}
