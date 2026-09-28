package com.ezsplit.controller;

import com.ezsplit.dto.request.CreateSettlementRequest;
import com.ezsplit.entity.User;
import com.ezsplit.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSettlementRequest req,
            @AuthenticationPrincipal User caller) {
        settlementService.record(groupId, req, caller.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Settlement recorded"));
    }
}
