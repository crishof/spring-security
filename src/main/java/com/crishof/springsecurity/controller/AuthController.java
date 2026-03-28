package com.crishof.springsecurity.controller;

import com.crishof.springsecurity.dto.AuthMeResponse;
import com.crishof.springsecurity.dto.AuthResponse;
import com.crishof.springsecurity.dto.LoginRequest;
import com.crishof.springsecurity.dto.LogoutRequest;
import com.crishof.springsecurity.dto.MessageResponse;
import com.crishof.springsecurity.dto.RefreshTokenRequest;
import com.crishof.springsecurity.security.principal.SecurityUser;
import com.crishof.springsecurity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for core authentication operations.
 * Groups login, logout, refresh, and current-user endpoints.
 * Other authentication endpoints are organized in:
 * - RegistrationController: signup, verify-email, resend-verification
 * - PasswordRecoveryController: forgot-password, reset-password
 * - InvitationsController: accept-invite
 * - AdminInvitationController: create-invitation
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Core authentication endpoints (login, logout, refresh)")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user with email and password.
     * Returns an access token and refresh token when credentials are valid.
     *
     * @param request email and password payload
     * @return authentication tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user and returns JWT tokens")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "403", description = "Account not verified or locked")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Logs out a user using the refresh token.
     * The token is revoked and cannot be used to obtain new access tokens.
     *
     * @param request refresh token
     * @return confirmation message
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logs out the user by revoking their refresh token")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Logout request received");
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    /**
     * Logs out from all user sessions.
     * Revokes all active refresh tokens.
     * Requires authentication.
     *
     * @param user authenticated user
     * @return confirmation message
     */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from all devices", description = "Revokes all refresh tokens for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Logged out from all devices")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal SecurityUser user) {
        log.info("Logout all sessions request for user: {}", user.getId());
        authService.logoutAll(user.getId());
        return ResponseEntity.ok(new MessageResponse("Logout from all devices successful"));
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * The refresh token must be active and not expired.
     *
     * @param request refresh token
     * @return new token pair
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request");
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    /**
     * Returns details about the authenticated user.
     * Requires a valid access token.
     *
     * @param user authenticated user extracted from JWT
     * @return user details
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get authenticated user profile",
            description = "Returns details about the currently authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "User profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal SecurityUser user) {
        log.debug("Getting user profile for: {}", user.getEmail());
        return ResponseEntity.ok(authService.me(user));
    }
}
