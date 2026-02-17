package com.eventhub.seat.repository;

import com.eventhub.seat.entity.Seat;
import com.eventhub.seat.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByEventId(UUID eventId);

    List<Seat> findByEventIdAndStatus(UUID eventId, SeatStatus status);

    List<Seat> findByEventIdAndSectionId(UUID eventId, String sectionId);

    List<Seat> findByIdIn(List<UUID> ids);

    @Query("SELECT s FROM Seat s WHERE s.eventId = :eventId AND s.id IN :seatIds")
    List<Seat> findByEventIdAndIdIn(@Param("eventId") UUID eventId, @Param("seatIds") List<UUID> seatIds);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.eventId = :eventId AND s.status = :status")
    long countByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") SeatStatus status);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedBy = NULL, s.lockedAt = NULL, " +
           "s.lockExpiresAt = NULL WHERE s.status = 'LOCKED' AND s.lockExpiresAt < :now")
    int releaseExpiredLocks(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedBy = NULL, s.lockedAt = NULL, " +
           "s.lockExpiresAt = NULL WHERE s.id IN :seatIds AND s.lockedBy = :userId")
    int releaseLocksByUser(@Param("seatIds") List<UUID> seatIds, @Param("userId") UUID userId);
}
