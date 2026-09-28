package com.ezsplit.dto.request;

import com.ezsplit.entity.ExpenseCategory;
import com.ezsplit.entity.SplitMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateExpenseRequest {
    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal total;

    @NotNull
    private UUID paidByUserId;

    @NotNull
    private SplitMethod splitMethod;

    private ExpenseCategory category;

    @NotEmpty
    @Valid
    private List<ParticipantShare> participants;

    @Data
    public static class ParticipantShare {
        @NotNull
        private UUID userId;

        /** Required when splitMethod = PERCENTAGE. Must sum to 100 across participants. */
        private BigDecimal percentage;

        /** Required when splitMethod = CUSTOM. Must sum to total across participants. */
        private BigDecimal amount;
    }
}
