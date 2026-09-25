package com.ezsplit.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID userId;
    private String username;
    private String email;
    private Instant createdAt;
}
