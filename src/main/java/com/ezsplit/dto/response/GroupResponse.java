package com.ezsplit.dto.response;

import com.ezsplit.entity.GroupCategory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GroupResponse {
    private UUID groupId;
    private String name;
    private String description;
    private GroupCategory category;
    private String currency;
    private UUID createdBy;
    private String createdByUsername;
    private Instant createdAt;
    private Instant updatedAt;
}
