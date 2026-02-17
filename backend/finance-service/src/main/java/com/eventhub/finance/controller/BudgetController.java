package com.eventhub.finance.controller;

import com.eventhub.finance.entity.Budget;
import com.eventhub.finance.entity.BudgetItem;
import com.eventhub.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<Page<Budget>> getOrganizerBudgets(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(budgetService.getOrganizerBudgets(UUID.fromString(userId), page, size));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Budget>> getEventBudgets(@PathVariable UUID eventId) {
        return ResponseEntity.ok(budgetService.getEventBudgets(eventId));
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<Budget> getBudget(@PathVariable UUID budgetId) {
        return ResponseEntity.ok(budgetService.getBudget(budgetId));
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Budget budget) {
        budget.setOrganizerId(UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(budget));
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable UUID budgetId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Budget updates) {
        return ResponseEntity.ok(budgetService.updateBudget(budgetId, updates, UUID.fromString(userId)));
    }

    // ─── Budget Items ───

    @GetMapping("/{budgetId}/items")
    public ResponseEntity<List<BudgetItem>> getBudgetItems(@PathVariable UUID budgetId) {
        return ResponseEntity.ok(budgetService.getBudgetItems(budgetId));
    }

    @PostMapping("/{budgetId}/items")
    public ResponseEntity<BudgetItem> addBudgetItem(
            @PathVariable UUID budgetId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody BudgetItem item) {
        item.setBudgetId(budgetId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.addBudgetItem(item, UUID.fromString(userId)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<BudgetItem> updateBudgetItem(
            @PathVariable UUID itemId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody BudgetItem updates) {
        return ResponseEntity.ok(budgetService.updateBudgetItem(itemId, updates, UUID.fromString(userId)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403, "message", e.getMessage(), "timestamp", Instant.now().toString()));
    }
}
