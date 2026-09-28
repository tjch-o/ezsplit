package com.ezsplit.controller;

import com.ezsplit.dto.response.BalanceResponse;
import com.ezsplit.entity.User;
import com.ezsplit.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
@RequiredArgsConstructor
public class BalanceController {
    private final BalanceService balanceService;

    @GetMapping
    public ResponseEntity<List<BalanceResponse>> getBalances(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(balanceService.getGroupBalances(groupId, caller.getUserId()));
    }
}
