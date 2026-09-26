package com.ezsplit.repository;

import com.ezsplit.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    @Query("""
        SELECT g FROM Group g
        JOIN GroupMember gm ON gm.group = g
        WHERE gm.user.userId = :userId
        ORDER BY g.updatedAt DESC
    """)
    List<Group> findAllByMemberUserId(@Param("userId") UUID userId);
}
