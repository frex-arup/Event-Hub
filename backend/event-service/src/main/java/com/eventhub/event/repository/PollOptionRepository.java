package com.eventhub.event.repository;

import com.eventhub.event.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    @Modifying
    @Query("UPDATE PollOption o SET o.voteCount = o.voteCount + 1 WHERE o.id = :optionId")
    void incrementVoteCount(@Param("optionId") UUID optionId);

    @Modifying
    @Query("UPDATE PollOption o SET o.voteCount = GREATEST(o.voteCount - 1, 0) WHERE o.id = :optionId")
    void decrementVoteCount(@Param("optionId") UUID optionId);
}
