package com.ezsplit.service;

import com.ezsplit.dto.request.CreateGroupRequest;
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
        User creator = userRepository.findById(creatorUserId).orElseThrow(() -> new BusinessException("Creator not found"));

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
