package com.ezsplit.service;

import com.ezsplit.dto.request.CreateSettlementRequest;
import com.ezsplit.entity.Group;
import com.ezsplit.entity.Settlement;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @InjectMocks SettlementService settlementService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId   = UUID.randomUUID();

    @Test
    void record_shouldPersistSettlement_whenInputIsValid() {
        stubValidParties();

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("20.00"));
        req.setNote("Cash");

        settlementService.record(groupId, req, bobId);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());

        Settlement saved = captor.getValue();
        assertThat(saved.getPayer().getUserId()).isEqualTo(bobId);
        assertThat(saved.getPayee().getUserId()).isEqualTo(aliceId);
        assertThat(saved.getAmount()).isEqualByComparingTo("20.00");
        assertThat(saved.getNote()).isEqualTo("Cash");
    }

    @Test
    void record_shouldScaleAmountToTwoDecimals() {
        stubValidParties();

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("20.555"));   // over-precise input

        settlementService.record(groupId, req, bobId);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("20.56");
    }

    @Test
    void record_shouldThrow_whenAmountIsZeroOrNegative() {
        // reuse the helper that stubs group + members + user lookups
        stubValidParties();

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("0.00"));

        assertThatThrownBy(() -> settlementService.record(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void record_shouldThrow_whenPayerEqualsPayee() {
        stubGroupExists();
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(true);
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(User.builder().userId(aliceId).username("alice").build()));

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);   // = payer
        req.setAmount(new BigDecimal("20.00"));

        assertThatThrownBy(() -> settlementService.record(groupId, req, aliceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot settle with yourself");
    }

    @Test
    void record_shouldThrow_whenGroupDoesNotExist() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("20.00"));

        assertThatThrownBy(() -> settlementService.record(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Group not found");
    }

    @Test
    void record_shouldThrow_whenCallerIsNotGroupMember() {
        stubGroupExists();
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bobId))
                .thenReturn(false);

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("20.00"));

        assertThatThrownBy(() -> settlementService.record(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void record_shouldThrow_whenPayeeIsNotGroupMember() {
        stubGroupExists();
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bobId))
                .thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(false);

        CreateSettlementRequest req = new CreateSettlementRequest();
        req.setPayeeUserId(aliceId);
        req.setAmount(new BigDecimal("20.00"));

        assertThatThrownBy(() -> settlementService.record(groupId, req, bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a member");
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private void stubGroupExists() {
        Group group = Group.builder().groupId(groupId).currency("SGD").build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    }

    private void stubValidParties() {
        stubGroupExists();
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bobId))
                .thenReturn(true);
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, aliceId))
                .thenReturn(true);
        when(userRepository.findById(bobId))
                .thenReturn(Optional.of(User.builder().userId(bobId).username("bob").build()));
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(User.builder().userId(aliceId).username("alice").build()));
    }
}
