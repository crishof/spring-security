package com.crishof.springsecurity.controller;

import com.crishof.springsecurity.dto.MessageResponse;
import com.crishof.springsecurity.dto.ResendVerificationRequest;
import com.crishof.springsecurity.dto.SignupRequest;
import com.crishof.springsecurity.dto.SignupResponse;
import com.crishof.springsecurity.dto.VerifyEmailRequest;
import com.crishof.springsecurity.dto.AuthResponse;
import com.crishof.springsecurity.service.AuthService;
import com.crishof.springsecurity.util.ValidationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for registration and email verification operations.
 * Groups signup and verification-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth/registration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Registration", description = "Endpoints for user registration and email verification")
public class RegistrationController {

    private final AuthService authService;

    /**
     * Registers a new user.
     * Requires email and password, then sends a verification code by email.
     *
     * @param request signup payload
     * @return created user and verification instructions
     */
    @PostMapping("/signup")
    @Operation(
            summary = "Register new user",
            description = "Creates a new user account. Email verification is required before login."
    )
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        log.info("Signup request for email: {}", request.email());

        ValidationUtil.validateEmail(request.email());
        ValidationUtil.validateFullName(request.fullName());
        ValidationUtil.validatePassword(request.password());

        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verifies user email using a 6-digit code.
     *
     * @param request email and verification code
     * @return authentication tokens
     */
    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify email address",
            description = "Verifies user's email using a 6-digit code sent to their email address."
    )
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.debug("Email verification request for: {}", request.email());

        ValidationUtil.validateEmail(request.email());
        ValidationUtil.validateVerificationCode(request.code());

        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resends the verification code to the user's email.
     * Useful when the original message was not received.
     *
     * @param request email payload
     * @return confirmation message
     */
    @PostMapping("/resend-verification")
    @Operation(
            summary = "Resend verification code",
            description = "Resends the email verification code to the user's email address."
    )
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        log.debug("Resend verification request for: {}", request.email());

        ValidationUtil.validateEmail(request.email());
        authService.resendEmailVerificationCode(request.email());

        return ResponseEntity.ok(new MessageResponse(
                "If an unverified account exists with this email, a new verification code has been sent"));
    }
}
