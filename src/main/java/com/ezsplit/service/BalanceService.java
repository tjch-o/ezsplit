package com.ezsplit.service;

import com.ezsplit.dto.response.BalanceResponse;
import com.ezsplit.entity.GroupMember;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;

    /**
     * Returns each member's net balance in the group.
     * Positive = is owed money; negative = owes money.
     */
    @Transactional(readOnly = true)
    public List<BalanceResponse> getGroupBalances(UUID groupId, UUID callerUserId) {
        if (!groupRepository.existsById(groupId)) {
            throw new BusinessException("Group not found");
        }
        if (!groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, callerUserId)) {
            throw new BusinessException("You do not have access to this group");
        }

        List<GroupMember> members = groupMemberRepository.findAllByGroupGroupId(groupId);
        List<BalanceResponse> result = new ArrayList<>();

        for (GroupMember m : members) {
            UUID uid = m.getUser().getUserId();

            BigDecimal paid = expenseRepository.sumPaidByUserInGroup(groupId, uid);
            BigDecimal owed = expenseSplitRepository.sumOwedByUserInGroup(groupId, uid);
            BigDecimal settledOut = settlementRepository.sumPaidByUserInGroup(groupId, uid);
            BigDecimal settledIn  = settlementRepository.sumReceivedByUserInGroup(groupId, uid);

            BigDecimal net = paid.subtract(owed).add(settledOut).subtract(settledIn);

            result.add(BalanceResponse.builder()
                    .userId(uid)
                    .username(m.getUser().getUsername())
                    .netBalance(net)
                    .build());
        }

        return result;
    }
}
