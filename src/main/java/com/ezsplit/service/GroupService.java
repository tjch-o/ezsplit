package com.ezsplit.service;

import com.ezsplit.dto.request.AddGroupMemberRequest;
import com.ezsplit.dto.request.CreateGroupRequest;
import com.ezsplit.dto.response.GroupMemberResponse;
import com.ezsplit.dto.response.GroupResponse;
import com.ezsplit.entity.Group;
import com.ezsplit.entity.GroupMember;
import com.ezsplit.entity.MembershipRole;
import com.ezsplit.entity.User;
import com.ezsplit.exception.BusinessException;
import com.ezsplit.repository.GroupMemberRepository;
import com.ezsplit.repository.GroupRepository;
import com.ezsplit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public GroupResponse create(CreateGroupRequest req, UUID creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new BusinessException("Creator not found"));

        Group group = Group.builder()
                .name(req.getName().trim())
                .description(req.getDescription() == null ? null : req.getDescription())
                .category(req.getCategory())
                .currency(req.getCurrency().toUpperCase())
                .createdBy(creator)
                .build();

        Group saved = groupRepository.saveAndFlush(group);

        GroupMember owner = GroupMember.builder()
                .group(saved)
                .user(creator)
                .role(MembershipRole.OWNER)
                .build();
        groupMemberRepository.save(owner);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups(UUID userId) {
        return groupRepository.findAllByMemberUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single group, but only if the given user is a member of it.
     *
     * @param groupId the group to fetch
     * @param userId  the authenticated user making the request
     * @return the group's details
     * @throws BusinessException if the group does not exist, or if the user is not a member
     */
    @Transactional(readOnly = true)
    public GroupResponse getById(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        if (!groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, userId)) {
            throw new BusinessException("You do not have access to this group");
        }
        return toResponse(group);
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, AddGroupMemberRequest req, UUID callerUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        GroupMember caller = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, callerUserId)
                .orElseThrow(() -> new BusinessException("You do not have access to this group"));
        ;

        if (caller.getRole() != MembershipRole.OWNER) {
            throw new BusinessException("Only group owners can add members");
        }

        User newMember = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, req.getUserId())) {
            throw new BusinessException("User is already a member of this group");
        }

        MembershipRole role = req.getRole() == null ? MembershipRole.MEMBER : req.getRole();
        GroupMember membership = GroupMember.builder()
                .user(newMember)
                .group(group)
                .role(role)
                .build();

        groupMemberRepository.save(membership);
        return toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(UUID groupId, UUID callerUserId) {
        if (!groupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, callerUserId)) {
            throw new BusinessException("You do not have access to this group");
        }

        return groupMemberRepository.findAllByGroupGroupId(groupId)
                .stream()
                .map(this::toGroupMemberResponse)
                .toList();
    }

    private GroupMemberResponse toGroupMemberResponse(GroupMember gm) {
        return GroupMemberResponse.builder()
                .userId(gm.getUser().getUserId())
                .username(gm.getUser().getUsername())
                .email(gm.getUser().getEmail())
                .role(gm.getRole())
                .joinedAt(gm.getJoinedAt())
                .build();
    }

    public GroupResponse toResponse(Group group) {
        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .name(group.getName())
                .description(group.getDescription())
                .category(group.getCategory())
                .currency(group.getCurrency())
                .createdBy(group.getCreatedBy().getUserId())
                .createdByUsername(group.getCreatedBy().getUsername())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
