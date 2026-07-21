package com.marketplace.user.dto;

import com.marketplace.user.entity.Role;
import java.time.Instant;

public record UserResponse(
        String id, String email, String fullName, String phone, Role role, Instant createdAt) {}
