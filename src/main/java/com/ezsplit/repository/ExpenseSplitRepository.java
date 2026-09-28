package com.ezsplit.repository;

import com.ezsplit.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {
    List<ExpenseSplit> findAllByExpenseExpenseId(UUID expenseId);

    @Query("""
        SELECT es FROM ExpenseSplit es
        JOIN FETCH es.user
        WHERE es.expense.expenseId IN :expenseIds
    """)
    List<ExpenseSplit> findAllWithUserByExpenseIds(@Param("expenseIds") Collection<UUID> expenseIds);

    /**
     * Total amount a user owes across all expenses in a group.
     */
    @Query("""
        SELECT COALESCE(SUM(es.amountOwed), 0)
        FROM ExpenseSplit es
        WHERE es.expense.group.groupId = :groupId
          AND es.user.userId = :userId
    """)
    BigDecimal sumOwedByUserInGroup(@Param("groupId") UUID groupId,
                                    @Param("userId") UUID userId);
    void deleteAllByExpenseExpenseId(UUID expenseId);
}