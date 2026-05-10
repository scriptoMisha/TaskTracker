package com.example.minitasks.user.dto;

import com.example.minitasks.user.Role;
import com.example.minitasks.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        boolean active,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
