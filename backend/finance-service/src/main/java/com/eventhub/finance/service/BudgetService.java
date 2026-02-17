package com.eventhub.finance.service;

import com.eventhub.finance.entity.Budget;
import com.eventhub.finance.entity.BudgetItem;
import com.eventhub.finance.repository.BudgetItemRepository;
import com.eventhub.finance.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetItemRepository budgetItemRepository;

    @Transactional(readOnly = true)
    public Page<Budget> getOrganizerBudgets(UUID organizerId, int page, int size) {
        return budgetRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<Budget> getEventBudgets(UUID eventId) {
        return budgetRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Budget getBudget(UUID budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + budgetId));
    }

    @Transactional
    public Budget createBudget(Budget budget) {
        budget = budgetRepository.save(budget);
        log.info("Budget created: {} for event {} by organizer {}", budget.getId(), budget.getEventId(), budget.getOrganizerId());
        return budget;
    }

    @Transactional
    public Budget updateBudget(UUID budgetId, Budget updates, UUID organizerId) {
        Budget budget = getBudget(budgetId);
        if (!budget.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized to update this budget");
        }
        if (updates.getName() != null) budget.setName(updates.getName());
        if (updates.getTotalBudget() != null) budget.setTotalBudget(updates.getTotalBudget());
        if (updates.getCurrency() != null) budget.setCurrency(updates.getCurrency());
        budget = budgetRepository.save(budget);
        log.info("Budget updated: {}", budget.getId());
        return budget;
    }

    // ─── Budget Items ───

    @Transactional(readOnly = true)
    public List<BudgetItem> getBudgetItems(UUID budgetId) {
        return budgetItemRepository.findByBudgetIdOrderByCreatedAtAsc(budgetId);
    }

    @Transactional
    public BudgetItem addBudgetItem(BudgetItem item, UUID organizerId) {
        Budget budget = getBudget(item.getBudgetId());
        if (!budget.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized");
        }
        item = budgetItemRepository.save(item);

        // Update spent total if actual_amount is set
        if (item.getActualAmount() != null) {
            recalculateSpent(budget);
        }

        log.info("Budget item added: {} to budget {}", item.getId(), item.getBudgetId());
        return item;
    }

    @Transactional
    public BudgetItem updateBudgetItem(UUID itemId, BudgetItem updates, UUID organizerId) {
        BudgetItem item = budgetItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Budget item not found: " + itemId));
        Budget budget = getBudget(item.getBudgetId());
        if (!budget.getOrganizerId().equals(organizerId)) {
            throw new SecurityException("Not authorized");
        }
        if (updates.getCategory() != null) item.setCategory(updates.getCategory());
        if (updates.getDescription() != null) item.setDescription(updates.getDescription());
        if (updates.getEstimatedAmount() != null) item.setEstimatedAmount(updates.getEstimatedAmount());
        if (updates.getActualAmount() != null) item.setActualAmount(updates.getActualAmount());
        item = budgetItemRepository.save(item);

        recalculateSpent(budget);
        return item;
    }

    private void recalculateSpent(Budget budget) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderByCreatedAtAsc(budget.getId());
        BigDecimal totalSpent = items.stream()
                .filter(i -> i.getActualAmount() != null)
                .map(BudgetItem::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        budget.setSpent(totalSpent);
        budgetRepository.save(budget);
    }
}
