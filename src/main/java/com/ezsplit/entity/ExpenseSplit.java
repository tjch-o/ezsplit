package com.ezsplit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "expense_splits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_split_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "split_id", updatable = false, nullable = false)
    private UUID splitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount_owed", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountOwed;
}
