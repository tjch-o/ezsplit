package com.ezsplit.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateSettlementRequest {
    @NotNull
    private UUID payeeUserId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String note;
}
