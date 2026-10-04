package com.ezsplit.service;

import com.ezsplit.dto.request.CreateExpenseRequest;
import com.ezsplit.entity.*;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseSplitRepository expenseSplitRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ExpenseService expenseService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId   = UUID.randomUUID();
    private final UUID carolId = UUID.randomUUID();

    @Test
    void equalSplit_shouldDivideEvenly_whenDivisible() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.EQUAL, "60.00", aliceId);
        req.setParticipants(List.of(share(aliceId), share(bobId), share(carolId)));

        expenseService.create(groupId, req, aliceId);

        List<ExpenseSplit> saved = captureSavedSplits();
        assertThat(saved).hasSize(3);
        saved.forEach(s -> assertThat(s.getAmountOwed()).isEqualByComparingTo("20.00"));
        assertThat(sumOf(saved)).isEqualByComparingTo("60.00");
    }

    @Test
    void equalSplit_shouldAbsorbRemainder_inLastParticipant() {
        setupGroupAndMembers();

        // $10 / 3 → 3.33, 3.33, 3.34
        CreateExpenseRequest req = baseRequest(SplitMethod.EQUAL, "10.00", aliceId);
        req.setParticipants(List.of(share(aliceId), share(bobId), share(carolId)));

        expenseService.create(groupId, req, aliceId);

        List<ExpenseSplit> saved = captureSavedSplits();
        assertThat(saved.get(0).getAmountOwed()).isEqualByComparingTo("3.33");
        assertThat(saved.get(1).getAmountOwed()).isEqualByComparingTo("3.33");
        assertThat(saved.get(2).getAmountOwed()).isEqualByComparingTo("3.34");
        assertThat(sumOf(saved)).isEqualByComparingTo("10.00");   // the invariant
    }

    @Test
    void percentageSplit_shouldComputeCorrectAmounts() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.PERCENTAGE, "100.00", aliceId);
        req.setParticipants(List.of(
                pct(aliceId, "50"),
                pct(bobId,   "30"),
                pct(carolId, "20")
        ));

        expenseService.create(groupId, req, aliceId);

        List<ExpenseSplit> saved = captureSavedSplits();
        assertThat(saved.get(0).getAmountOwed()).isEqualByComparingTo("50.00");
        assertThat(saved.get(1).getAmountOwed()).isEqualByComparingTo("30.00");
        assertThat(saved.get(2).getAmountOwed()).isEqualByComparingTo("20.00");
    }

    @Test
    void percentageSplit_shouldThrow_whenPercentagesDoNotSumTo100() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.PERCENTAGE, "100.00", aliceId);
        req.setParticipants(List.of(
                pct(aliceId, "50"),
                pct(bobId,   "30"),
                pct(carolId, "30")   // 110
        ));

        assertThatThrownBy(() -> expenseService.create(groupId, req, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Percentages must sum to 100");
    }

    @Test
    void percentageSplit_shouldHandleRoundingDrift() {
        setupGroupAndMembers();

        // $10 split as 33.33/33.33/33.34
        CreateExpenseRequest req = baseRequest(SplitMethod.PERCENTAGE, "10.00", aliceId);
        req.setParticipants(List.of(
                pct(aliceId, "33.33"),
                pct(bobId,   "33.33"),
                pct(carolId, "33.34")
        ));

        expenseService.create(groupId, req, aliceId);

        List<ExpenseSplit> saved = captureSavedSplits();
        assertThat(sumOf(saved)).isEqualByComparingTo("10.00");
    }

    @Test
    void customSplit_shouldStoreAmountsAsProvided() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.CUSTOM, "30.00", bobId);
        req.setParticipants(List.of(
                amt(aliceId, "8.00"),
                amt(bobId,   "12.00"),
                amt(carolId, "10.00")
        ));

        expenseService.create(groupId, req, bobId);

        List<ExpenseSplit> saved = captureSavedSplits();
        assertThat(saved.get(0).getAmountOwed()).isEqualByComparingTo("8.00");
        assertThat(saved.get(1).getAmountOwed()).isEqualByComparingTo("12.00");
        assertThat(saved.get(2).getAmountOwed()).isEqualByComparingTo("10.00");
    }

    @Test
    void customSplit_shouldThrow_whenAmountsDoNotSumToTotal() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.CUSTOM, "30.00", bobId);
        req.setParticipants(List.of(
                amt(aliceId, "8.00"),
                amt(bobId,   "12.00"),
                amt(carolId, "9.00")   // sums to 29
        ));

        assertThatThrownBy(() -> expenseService.create(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Custom amounts must sum to the total");
    }

    @Test
    void customSplit_shouldThrow_whenAmountIsNegative() {
        setupGroupAndMembers();

        CreateExpenseRequest req = baseRequest(SplitMethod.CUSTOM, "30.00", bobId);
        req.setParticipants(List.of(
                amt(aliceId, "-5.00"),
                amt(bobId,   "20.00"),
                amt(carolId, "15.00")
        ));

        assertThatThrownBy(() -> expenseService.create(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void create_shouldThrow_whenCallerIsNotGroupMember() {
        Group group = Group.builder().groupId(groupId).currency("SGD").build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(false);

        CreateExpenseRequest req = baseRequest(SplitMethod.EQUAL, "60.00", aliceId);
        req.setParticipants(List.of(share(aliceId)));

        assertThatThrownBy(() -> expenseService.create(groupId, req, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void create_shouldThrow_whenParticipantIsNotGroupMember() {
        Group group = Group.builder().groupId(groupId).currency("SGD").build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(true);
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(User.builder().userId(aliceId).username("alice").build()));
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bobId))
                .thenReturn(false);

        CreateExpenseRequest req = baseRequest(SplitMethod.EQUAL, "60.00", aliceId);
        req.setParticipants(List.of(share(aliceId), share(bobId)));

        assertThatThrownBy(() -> expenseService.create(groupId, req, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a member");
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private void setupGroupAndMembers() {
        Group group = Group.builder().groupId(groupId).currency("SGD").build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId)).thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bobId)).thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, carolId)).thenReturn(true);

        // use lenient as not every test reaches these lookups
        lenient().when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(User.builder().userId(aliceId).username("alice").build()));
        lenient().when(userRepository.findById(bobId))
                .thenReturn(Optional.of(User.builder().userId(bobId).username("bob").build()));
        lenient().when(userRepository.findById(carolId))
                .thenReturn(Optional.of(User.builder().userId(carolId).username("carol").build()));

        lenient().when(expenseRepository.saveAndFlush(any(Expense.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateExpenseRequest baseRequest(SplitMethod method, String total, UUID payerId) {
        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setDescription("Test expense");
        req.setTotal(new BigDecimal(total));
        req.setPaidByUserId(payerId);
        req.setSplitMethod(method);
        return req;
    }

    private CreateExpenseRequest.ParticipantShare share(UUID userId) {
        var p = new CreateExpenseRequest.ParticipantShare();
        p.setUserId(userId);
        return p;
    }

    private CreateExpenseRequest.ParticipantShare pct(UUID userId, String pct) {
        var p = new CreateExpenseRequest.ParticipantShare();
        p.setUserId(userId);
        p.setPercentage(new BigDecimal(pct));
        return p;
    }

    private CreateExpenseRequest.ParticipantShare amt(UUID userId, String amt) {
        var p = new CreateExpenseRequest.ParticipantShare();
        p.setUserId(userId);
        p.setAmount(new BigDecimal(amt));
        return p;
    }

    @SuppressWarnings("unchecked")
    private List<ExpenseSplit> captureSavedSplits() {
        ArgumentCaptor<List<ExpenseSplit>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseSplitRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private BigDecimal sumOf(List<ExpenseSplit> splits) {
        return splits.stream()
                .map(ExpenseSplit::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
