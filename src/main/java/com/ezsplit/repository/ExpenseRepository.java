package com.ezsplit.repository;

import com.ezsplit.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findAllByGroupGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * Total amount a user has paid in a group.
     */
    @Query("""
        SELECT COALESCE(SUM(e.total), 0)
        FROM Expense e
        WHERE e.group.groupId = :groupId
          AND e.paidBy.userId = :userId
    """)
    BigDecimal sumPaidByUserInGroup(@Param("groupId") UUID groupId,
                                    @Param("userId") UUID userId);
}
