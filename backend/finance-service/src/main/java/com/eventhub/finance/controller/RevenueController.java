package com.eventhub.finance.controller;

import com.eventhub.finance.entity.RevenueRecord;
import com.eventhub.finance.entity.Settlement;
import com.eventhub.finance.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/revenue/event/{eventId}")
    public ResponseEntity<Page<RevenueRecord>> getEventRevenue(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(revenueService.getEventRevenue(eventId, page, size));
    }

    @GetMapping("/revenue/organizer")
    public ResponseEntity<Page<RevenueRecord>> getOrganizerRevenue(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(revenueService.getOrganizerRevenue(UUID.fromString(userId), page, size));
    }

    @PostMapping("/revenue")
    public ResponseEntity<RevenueRecord> recordRevenue(@RequestBody RevenueRecord record) {
        return ResponseEntity.status(HttpStatus.CREATED).body(revenueService.recordRevenue(record));
    }

    @GetMapping("/analytics/event/{eventId}")
    public ResponseEntity<Map<String, Object>> getEventAnalytics(@PathVariable UUID eventId) {
        return ResponseEntity.ok(revenueService.getEventAnalytics(eventId));
    }

    @GetMapping("/analytics/organizer")
    public ResponseEntity<Map<String, Object>> getOrganizerAnalytics(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(revenueService.getOrganizerAnalytics(UUID.fromString(userId)));
    }

    // ─── Settlements ───

    @GetMapping("/settlements")
    public ResponseEntity<Page<Settlement>> getSettlements(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueService.getOrganizerSettlements(UUID.fromString(userId), page, size));
    }

    @PostMapping("/settlements")
    public ResponseEntity<Settlement> createSettlement(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Settlement settlement) {
        settlement.setOrganizerId(UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(revenueService.createSettlement(settlement));
    }

    @PostMapping("/settlements/{settlementId}/process")
    public ResponseEntity<Settlement> processSettlement(
            @PathVariable UUID settlementId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(revenueService.processSettlement(settlementId, body.get("payoutRef")));
    }

    @GetMapping("/settlements/pending")
    public ResponseEntity<List<Settlement>> getPendingSettlements() {
        return ResponseEntity.ok(revenueService.getPendingSettlements());
    }
}
