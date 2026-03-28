package com.crishof.springsecurity.dto;

import java.util.UUID;

public record SignupResponse(
        UUID userId,
        String email,
        String message,
        boolean emailVerificationRequired
) {
}
