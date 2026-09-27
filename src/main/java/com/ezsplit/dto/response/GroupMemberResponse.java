package com.ezsplit.dto.response;

import com.ezsplit.entity.MembershipRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GroupMemberResponse {
    private UUID userId;
    private String username;
    private String email;
    private MembershipRole role;
    private Instant joinedAt;
}
