package com.eventhub.booking.repository;

import com.eventhub.booking.entity.WaitlistEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    Optional<WaitlistEntry> findByEventIdAndUserId(UUID eventId, UUID userId);

    Page<WaitlistEntry> findByEventIdAndStatusOrderByCreatedAtAsc(UUID eventId, String status, Pageable pageable);

    Page<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<WaitlistEntry> findTop10ByEventIdAndStatusOrderByCreatedAtAsc(UUID eventId, String status);

    long countByEventIdAndStatus(UUID eventId, String status);

    @Modifying
    @Query("UPDATE WaitlistEntry w SET w.status = :status WHERE w.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status);
}
