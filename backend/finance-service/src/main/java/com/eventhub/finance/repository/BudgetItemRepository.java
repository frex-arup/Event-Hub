package com.eventhub.finance.repository;

import com.eventhub.finance.entity.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, UUID> {

    List<BudgetItem> findByBudgetIdOrderByCreatedAtAsc(UUID budgetId);

    void deleteByBudgetId(UUID budgetId);
}
