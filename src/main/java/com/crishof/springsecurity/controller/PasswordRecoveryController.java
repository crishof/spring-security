package com.crishof.springsecurity.controller;

import com.crishof.springsecurity.dto.ForgotPasswordRequest;
import com.crishof.springsecurity.dto.MessageResponse;
import com.crishof.springsecurity.dto.ResetPasswordRequest;
import com.crishof.springsecurity.service.AuthService;
import com.crishof.springsecurity.util.ValidationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for password recovery operations.
 * Groups endpoints related to password reset requests and completion.
 */
@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password Recovery", description = "Endpoints for password recovery operations")
public class PasswordRecoveryController {

    private final AuthService authService;

    /**
     * Requests a password reset link.
     * Sends an email with a reset token when the account exists.
     *
     * @param request email payload
     * @return confirmation message
     */
    @PostMapping("/forgot")
    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset link to the user's email. Returns success even if email doesn't exist for security."
    )
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.debug("Forgot password request for email: {}", request.email());

        ValidationUtil.validateEmail(request.email());
        authService.forgotPassword(request.email());

        return ResponseEntity.ok(new MessageResponse(
                "If an account exists with this email, a password reset link has been sent"));
    }

    /**
     * Resets the password using a valid token.
     *
     * @param request token, new password, and confirmation
     * @return confirmation message
     */
    @PostMapping("/reset")
    @Operation(
            summary = "Reset password with token",
            description = "Resets password using a valid reset token. Token must not be expired or already used."
    )
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Reset password request submitted");

        ValidationUtil.validatePassword(request.newPassword());
        ValidationUtil.validateEqualityOf(
                request.newPassword(),
                request.confirmPassword(),
                "Password"
        );

        authService.resetPassword(request);

        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully. You can now login with your new password."));
    }
}
