package com.ezsplit.dto.response;

import com.ezsplit.entity.ExpenseCategory;
import com.ezsplit.entity.SplitMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {
    private UUID expenseId;
    private UUID groupId;
    private String description;
    private BigDecimal total;
    private String currency;
    private ExpenseCategory category;
    private SplitMethod splitMethod;
    private UUID paidByUserId;
    private String paidByUsername;
    private Instant createdAt;
    private List<Share> shares;

    @Data
    @Builder
    public static class Share {
        private UUID userId;
        private String username;
        private BigDecimal amountOwed;
    }
}
