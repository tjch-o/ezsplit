package com.ezsplit.service;

import com.ezsplit.dto.response.BalanceResponse;
import com.ezsplit.entity.GroupMember;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseSplitRepository expenseSplitRepository;
    @Mock SettlementRepository settlementRepository;
    @InjectMocks BalanceService balanceService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId   = UUID.randomUUID();
    private final UUID carolId = UUID.randomUUID();

    @Test
    void shouldComputeBalance_forSingleExpense_splitEqually() {
        // Alice paid $60, everyone owes $20 → Alice +40, Bob -20, Carol -20
        stubMembership(groupId, aliceId, aliceId);
        stubMembers(aliceId, bobId, carolId);

        when(expenseRepository.sumPaidByUserInGroup(groupId, aliceId)).thenReturn(new BigDecimal("60.00"));
        when(expenseRepository.sumPaidByUserInGroup(groupId, bobId)).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumPaidByUserInGroup(groupId, carolId)).thenReturn(BigDecimal.ZERO);

        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, aliceId)).thenReturn(new BigDecimal("20.00"));
        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, bobId)).thenReturn(new BigDecimal("20.00"));
        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, carolId)).thenReturn(new BigDecimal("20.00"));

        stubZeroSettlements(aliceId, bobId, carolId);

        List<BalanceResponse> balances = balanceService.getGroupBalances(groupId, aliceId);

        assertThat(balances).hasSize(3);
        assertThat(balanceOf(balances, aliceId)).isEqualByComparingTo("40.00");
        assertThat(balanceOf(balances, bobId)).isEqualByComparingTo("-20.00");
        assertThat(balanceOf(balances, carolId)).isEqualByComparingTo("-20.00");

        // net balances always sum to zero
        assertThat(sumOfNet(balances)).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldComputeBalance_withSettlements() {
        // Alice paid $60; Bob and Carol each owe $20.
        // Bob settles $20 to Alice. Now Alice +20, Bob 0, Carol -20.
        stubMembership(groupId, aliceId, aliceId);
        stubMembers(aliceId, bobId, carolId);

        when(expenseRepository.sumPaidByUserInGroup(groupId, aliceId)).thenReturn(new BigDecimal("60.00"));
        when(expenseRepository.sumPaidByUserInGroup(groupId, bobId)).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumPaidByUserInGroup(groupId, carolId)).thenReturn(BigDecimal.ZERO);

        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, aliceId)).thenReturn(new BigDecimal("20.00"));
        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, bobId)).thenReturn(new BigDecimal("20.00"));
        when(expenseSplitRepository.sumOwedByUserInGroup(groupId, carolId)).thenReturn(new BigDecimal("20.00"));

        // Bob paid $20 settlement
        when(settlementRepository.sumPaidByUserInGroup(groupId, aliceId)).thenReturn(BigDecimal.ZERO);
        when(settlementRepository.sumPaidByUserInGroup(groupId, bobId)).thenReturn(new BigDecimal("20.00"));
        when(settlementRepository.sumPaidByUserInGroup(groupId, carolId)).thenReturn(BigDecimal.ZERO);

        // Alice received $20
        when(settlementRepository.sumReceivedByUserInGroup(groupId, aliceId)).thenReturn(new BigDecimal("20.00"));
        when(settlementRepository.sumReceivedByUserInGroup(groupId, bobId)).thenReturn(BigDecimal.ZERO);
        when(settlementRepository.sumReceivedByUserInGroup(groupId, carolId)).thenReturn(BigDecimal.ZERO);

        List<BalanceResponse> balances = balanceService.getGroupBalances(groupId, aliceId);

        assertThat(balanceOf(balances, aliceId)).isEqualByComparingTo("20.00");
        assertThat(balanceOf(balances, bobId)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(balances, carolId)).isEqualByComparingTo("-20.00");
        assertThat(sumOfNet(balances)).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnZeroBalances_whenNoExpenses() {
        stubMembership(groupId, aliceId, aliceId);
        stubMembers(aliceId, bobId, carolId);

        // Defaults for mocks that return BigDecimal are null unless stubbed.
        // Stub everything to zero for a clean empty-group scenario.
        for (UUID uid : List.of(aliceId, bobId, carolId)) {
            when(expenseRepository.sumPaidByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
            when(expenseSplitRepository.sumOwedByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
            when(settlementRepository.sumPaidByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
            when(settlementRepository.sumReceivedByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
        }

        List<BalanceResponse> balances = balanceService.getGroupBalances(groupId, aliceId);

        balances.forEach(b -> assertThat(b.getNetBalance()).isEqualByComparingTo("0.00"));
    }

    // ─────────────────────────────────────────────────────────
    // Authorization
    // ─────────────────────────────────────────────────────────
    @Test
    void shouldThrow_whenGroupDoesNotExist() {
        when(groupRepository.existsById(groupId)).thenReturn(false);

        assertThatThrownBy(() -> balanceService.getGroupBalances(groupId, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void shouldThrow_whenCallerIsNotMember() {
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(false);

        assertThatThrownBy(() -> balanceService.getGroupBalances(groupId, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not have access");
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private void stubMembership(UUID gid, UUID uid, UUID callerId) {
        when(groupRepository.existsById(gid)).thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(gid, callerId)).thenReturn(true);
    }

    private void stubMembers(UUID... uids) {
        List<GroupMember> members = java.util.Arrays.stream(uids)
                .map(uid -> GroupMember.builder()
                        .user(User.builder().userId(uid).username("user-" + uid).build())
                        .build())
                .toList();
        when(groupMemberRepository.findAllByGroupGroupId(groupId)).thenReturn(members);
    }

    private void stubZeroSettlements(UUID... uids) {
        for (UUID uid : uids) {
            when(settlementRepository.sumPaidByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
            when(settlementRepository.sumReceivedByUserInGroup(groupId, uid)).thenReturn(BigDecimal.ZERO);
        }
    }

    private BigDecimal balanceOf(List<BalanceResponse> balances, UUID userId) {
        return balances.stream()
                .filter(b -> b.getUserId().equals(userId))
                .findFirst()
                .orElseThrow()
                .getNetBalance();
    }

    private BigDecimal sumOfNet(List<BalanceResponse> balances) {
        return balances.stream()
                .map(BalanceResponse::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
