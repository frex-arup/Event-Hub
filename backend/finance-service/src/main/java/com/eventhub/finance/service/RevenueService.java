package com.eventhub.finance.service;

import com.eventhub.finance.entity.RevenueRecord;
import com.eventhub.finance.entity.Settlement;
import com.eventhub.finance.repository.RevenueRecordRepository;
import com.eventhub.finance.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final RevenueRecordRepository revenueRepository;
    private final SettlementRepository settlementRepository;

    // ─── Revenue Records ───

    @Transactional
    public RevenueRecord recordRevenue(RevenueRecord record) {
        record = revenueRepository.save(record);
        log.info("Revenue recorded: {} {} for event {} (type={})",
                record.getCurrency(), record.getAmount(), record.getEventId(), record.getType());
        return record;
    }

    @Transactional(readOnly = true)
    public Page<RevenueRecord> getEventRevenue(UUID eventId, int page, int size) {
        return revenueRepository.findByEventIdOrderByRecordedAtDesc(eventId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<RevenueRecord> getOrganizerRevenue(UUID organizerId, int page, int size) {
        return revenueRepository.findByOrganizerIdOrderByRecordedAtDesc(organizerId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventAnalytics(UUID eventId) {
        BigDecimal totalRevenue = revenueRepository.sumRevenueByEventId(eventId);
        BigDecimal totalRefunds = revenueRepository.sumRefundsByEventId(eventId);
        long totalBookings = revenueRepository.countByEventId(eventId);
        BigDecimal netRevenue = totalRevenue.subtract(totalRefunds);

        return Map.of(
                "eventId", eventId.toString(),
                "totalRevenue", totalRevenue,
                "totalRefunds", totalRefunds,
                "netRevenue", netRevenue,
                "totalBookings", totalBookings
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizerAnalytics(UUID organizerId) {
        BigDecimal totalRevenue = revenueRepository.sumRevenueByOrganizerId(organizerId);
        return Map.of(
                "organizerId", organizerId.toString(),
                "totalRevenue", totalRevenue
        );
    }

    // ─── Settlements ───

    @Transactional
    public Settlement createSettlement(Settlement settlement) {
        settlement = settlementRepository.save(settlement);
        log.info("Settlement created: {} for organizer {}", settlement.getId(), settlement.getOrganizerId());
        return settlement;
    }

    @Transactional(readOnly = true)
    public Page<Settlement> getOrganizerSettlements(UUID organizerId, int page, int size) {
        return settlementRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId, PageRequest.of(page, size));
    }

    @Transactional
    public Settlement processSettlement(UUID settlementId, String payoutRef) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        settlement.setStatus("COMPLETED");
        settlement.setPayoutRef(payoutRef);
        settlement.setSettledAt(Instant.now());
        settlement = settlementRepository.save(settlement);

        log.info("Settlement processed: {} ref={}", settlementId, payoutRef);
        return settlement;
    }

    @Transactional(readOnly = true)
    public List<Settlement> getPendingSettlements() {
        return settlementRepository.findByStatus("PENDING");
    }
}
