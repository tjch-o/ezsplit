package com.ezsplit.controller;

import com.ezsplit.dto.request.CreateExpenseRequest;
import com.ezsplit.dto.response.ExpenseResponse;
import com.ezsplit.entity.User;
import com.ezsplit.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateExpenseRequest req,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(groupId, req, caller.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> list(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(expenseService.listForGroup(groupId, caller.getUserId()));
    }
}
