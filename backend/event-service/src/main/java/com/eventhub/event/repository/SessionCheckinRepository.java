package com.eventhub.event.repository;

import com.eventhub.event.entity.SessionCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionCheckinRepository extends JpaRepository<SessionCheckin, UUID> {

    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    List<SessionCheckin> findBySessionId(UUID sessionId);

    List<SessionCheckin> findByUserId(UUID userId);

    long countBySessionId(UUID sessionId);
}
