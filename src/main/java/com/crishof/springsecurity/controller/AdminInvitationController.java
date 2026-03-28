package com.crishof.springsecurity.controller;

import com.crishof.springsecurity.dto.CreateInvitationRequest;
import com.crishof.springsecurity.dto.InvitationResponse;
import com.crishof.springsecurity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for administrative invitation operations.
 * Accessible only to users with the ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Invitations", description = "Administrative endpoints for managing user invitations")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminInvitationController {

    private final AuthService authService;

    /**
     * Creates a new invitation and sends it by email.
     * Accessible only to administrators.
     * The invited user receives an email with a link to accept the invitation.
     *
     * @param request invitation request (email + role)
     * @return created invitation
     */
    @PostMapping
    @Operation(
            summary = "Create user invitation",
            description = "Creates a new invitation and sends it to the specified email. Requires ADMIN role."
    )
    @ApiResponse(responseCode = "201", description = "Invitation created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "403", description = "Only admins can create invitations")
    @ApiResponse(responseCode = "409", description = "Email already registered or invitation pending")
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request) {
        log.info("Creating invitation for email: {} with role: {}", request.email(), request.role());
        InvitationResponse response = authService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
