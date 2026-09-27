package com.ezsplit.dto.request;

import com.ezsplit.entity.MembershipRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddGroupMemberRequest {
    @NotNull
    private UUID userId;

    /**
     * Optional. Defaults to MEMBER if not provided.
     * Only OWNERs of a group can promote someone to OWNER.
     */
    private MembershipRole role;
}
