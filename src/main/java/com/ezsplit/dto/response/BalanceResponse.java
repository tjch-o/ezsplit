package com.ezsplit.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BalanceResponse {
    private UUID userId;
    private String username;
    /**
     * Positive = user is owed money (they paid more than their share).
     * Negative = user owes money.
     * Zero = user has settled everything.
     */
    private BigDecimal netBalance;
}
