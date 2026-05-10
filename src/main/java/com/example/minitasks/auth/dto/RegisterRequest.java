package com.example.minitasks.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
