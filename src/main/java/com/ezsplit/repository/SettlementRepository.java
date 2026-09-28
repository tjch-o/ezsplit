package com.ezsplit.repository;

import com.ezsplit.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findAllByGroupGroupId(UUID groupId);

    /** How much has this user paid out as settlement in this group. */
    @Query("""
        SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s
        WHERE s.group.groupId = :groupId AND s.payer.userId = :userId
    """)
    BigDecimal sumPaidByUserInGroup(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    /** How much has this user received as settlement in this group. */
    @Query("""
        SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s
        WHERE s.group.groupId = :groupId AND s.payee.userId = :userId
    """)
    BigDecimal sumReceivedByUserInGroup(@Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
