package com.ezsplit.repository;

import com.ezsplit.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    boolean existsByGroupGroupIdAndUserUserId(UUID groupId, UUID userId);

    /**
     * Finds all membership records for a group — one per member.
     * Each record includes the user, their role, and when they joined.
     *
     * @param groupId the group whose members to list
     * @return list of memberships in that group; empty if the group has no members
     */
    List<GroupMember> findAllByGroupGroupId(UUID groupId);

    /**
     * Finds all membership records for a user — one per group they belong to.
     * Each record includes the group, the user, their role (OWNER/MEMBER), and when they joined.
     *
     * @param userId the user whose memberships to look up
     * @return list of memberships for that user; empty if they belong to no groups
     */
    List<GroupMember> findAllByUserUserId(UUID userId);
}
