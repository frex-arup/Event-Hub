package com.eventhub.booking.repository;

import com.eventhub.booking.entity.Booking;
import com.eventhub.booking.entity.BookingStatus;
import com.eventhub.booking.entity.SagaState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Booking> findByEventId(UUID eventId, Pageable pageable);

    List<Booking> findByEventIdAndStatus(UUID eventId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.sagaState = :state AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("state") SagaState state, @Param("now") Instant now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.eventId = :eventId AND b.status = 'CONFIRMED'")
    long countConfirmedByEvent(@Param("eventId") UUID eventId);
}
