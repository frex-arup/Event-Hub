package com.eventhub.finance.repository;

import com.eventhub.finance.entity.RevenueRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RevenueRecordRepository extends JpaRepository<RevenueRecord, UUID> {

    Page<RevenueRecord> findByEventIdOrderByRecordedAtDesc(UUID eventId, Pageable pageable);

    Page<RevenueRecord> findByOrganizerIdOrderByRecordedAtDesc(UUID organizerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RevenueRecord r WHERE r.eventId = :eventId AND r.type = 'TICKET_SALE'")
    BigDecimal sumRevenueByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RevenueRecord r WHERE r.eventId = :eventId AND r.type = 'REFUND'")
    BigDecimal sumRefundsByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RevenueRecord r WHERE r.organizerId = :organizerId AND r.type = 'TICKET_SALE'")
    BigDecimal sumRevenueByOrganizerId(@Param("organizerId") UUID organizerId);

    long countByEventId(UUID eventId);
}
