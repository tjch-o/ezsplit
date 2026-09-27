package com.ezsplit.service;

import com.ezsplit.dto.request.AddGroupMemberRequest;
import com.ezsplit.entity.Group;
import com.ezsplit.entity.GroupMember;
import com.ezsplit.entity.MembershipRole;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.GroupMemberRepository;
import com.ezsplit.repository.GroupRepository;
import com.ezsplit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ezsplit.entity.User;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    GroupRepository groupRepository;
    @Mock
    GroupMemberRepository groupMemberRepository;
    @InjectMocks
    GroupService groupService;

    @Test
    void getById_shouldThrow_whenCallerIsNotMember() {
        UUID groupId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();

        Group group = Group.builder().groupId(groupId).build();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, outsiderId))
                .thenReturn(false);

        assertThatThrownBy(() -> groupService.getById(groupId, outsiderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You do not have access to this group");
    }

    @Test
    void getById_shouldThrow_whenGroupDoesNotExist() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getById(groupId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Group not found");
    }

    @Test
    void addMember_shouldThrow_whenCallerIsNotOwner() {
        UUID groupId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        UUID newMemberId = UUID.randomUUID();

        Group group = Group.builder().groupId(groupId).build();
        GroupMember callerMembership = GroupMember.builder()
                .group(group)
                .role(MembershipRole.MEMBER)   // not OWNER
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, callerId))
                .thenReturn(Optional.of(callerMembership));

        AddGroupMemberRequest req = new AddGroupMemberRequest();
        req.setUserId(newMemberId);

        assertThatThrownBy(() -> groupService.addMember(groupId, req, callerId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only group owners can add members");
    }

    @Test
    void addMember_shouldThrow_whenUserIsAlreadyMember() {
        UUID groupId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        UUID existingMemberId = UUID.randomUUID();

        Group group = Group.builder().groupId(groupId).build();
        GroupMember caller = GroupMember.builder().role(MembershipRole.OWNER).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, callerId))
                .thenReturn(Optional.of(caller));
        when(userRepository.findById(existingMemberId))
                .thenReturn(Optional.of(User.builder().userId(existingMemberId).build()));
        when(groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, existingMemberId))
                .thenReturn(true);

        AddGroupMemberRequest req = new AddGroupMemberRequest();
        req.setUserId(existingMemberId);

        assertThatThrownBy(() -> groupService.addMember(groupId, req, callerId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User is already a member of this group");
    }
}
