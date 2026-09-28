package com.ezsplit.service;

import com.ezsplit.dto.request.CreateSettlementRequest;
import com.ezsplit.entity.Group;
import com.ezsplit.entity.Settlement;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(UUID groupId, CreateSettlementRequest req, UUID callerUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        requireMember(groupId, callerUserId);
        requireMember(groupId, req.getPayeeUserId());

        User payer = userRepository.findById(callerUserId)
                .orElseThrow(() -> new BusinessException("Payer not found"));
        User payee = userRepository.findById(req.getPayeeUserId())
                .orElseThrow(() -> new BusinessException("Payee not found"));

        if (payer.getUserId().equals(payee.getUserId())) {
            throw new BusinessException("You cannot settle with yourself");
        }

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }

        Settlement s = Settlement.builder()
                .group(group)
                .payer(payer)
                .payee(payee)
                .amount(amount)
                .note(req.getNote())
                .build();
        settlementRepository.save(s);
    }

    private void requireMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, userId)) {
            throw new BusinessException("User " + userId + " is not a member of this group");
        }
    }
}
