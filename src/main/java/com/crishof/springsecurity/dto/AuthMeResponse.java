package com.crishof.springsecurity.dto;

import java.util.UUID;

public record AuthMeResponse(UUID id, String email, String role, String status) {
}
