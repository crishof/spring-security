package com.crishof.springsecurity.service;

import com.crishof.springsecurity.dto.*;
import com.crishof.springsecurity.exception.*;
import com.crishof.springsecurity.model.*;
import com.crishof.springsecurity.repository.*;
import com.crishof.springsecurity.security.jwt.JwtService;
import com.crishof.springsecurity.security.principal.SecurityUser;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int VERIFICATION_CODE_DIGITS = 6;
    private static final long PASSWORD_RESET_MINUTES = 30;
    private static final long INVITATION_DAYS = 7;

    private final UserRepository userRepository;
    private final SecurityAccountRepository securityAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${app.email-verification.code-ttl-minutes:10}")
    private long emailVerificationCodeTtlMinutes;

    @Override
    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        User savedUser = userRepository.save(user);

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser)
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);

        issueEmailVerificationCode(savedUser);

        log.info("User signed up successfully. Verification required for email={}", normalizedEmail);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                "Signup successful. Please verify your email using the code sent.",
                true);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            normalizedEmail, request.password()));
        } catch (DisabledException _) {
            throw new AccountNotVerifiedException("Email verification is required before login");
        } catch (BadCredentialsException _) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        User user = getUserByEmailOrThrow(normalizedEmail);
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        validateAccountCanAuthenticate(user, account);

        return issueAuthTokens(user, account, true);
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        try {
            if (!jwtService.isTokenValid(refreshTokenValue)) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).orElseThrow(
                    () -> new InvalidTokenException("Invalid or expired refresh token"));

            if (!storedToken.isValid()) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }

            User user = storedToken.getUser();
            SecurityAccount account = getSecurityAccountByUserOrThrow(user);

            validateAccountCanAuthenticate(user, account);

            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            return issueAuthTokens(user, account, true);
        } catch (JwtException | IllegalArgumentException _) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseThrow(
                () -> new InvalidTokenException("Invalid or expired verification code"));

        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(user, request.code())
                        .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification code"));

        if (!verificationToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired verification code");
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        emailVerificationTokenRepository.deleteByUser(user);

        account.setEmailVerified(true);
        securityAccountRepository.save(account);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return issueAuthTokens(user, account, true);
    }

    @Override
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        InvitationToken invitationToken = invitationTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation token"));

        if (!invitationToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired invitation token");
        }

        if (!invitationToken.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BusinessException("Invitation email does not match the provided email");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        User user = new User();
        user.setFullName(normalizeFullName(request.fullName()));
        user.setEmail(normalizedEmail);
        user.setRole(invitationToken.getRole());
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        SecurityAccount account = SecurityAccount.builder()
                .user(savedUser).passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(true)
                .enabled(true)
                .locked(false)
                .build();

        securityAccountRepository.save(account);

        invitationToken.setUsed(true);
        invitationTokenRepository.save(invitationToken);

        return issueAuthTokens(savedUser, account, true);
    }

    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existing email={}", normalizedEmail);
            return;
        }

        User user = userOptional.get();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (!account.isEnabled()) {
            log.info("Password reset ignored for disabled account email={}", normalizedEmail);
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(PASSWORD_RESET_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("Password reset token generated for user {}", user.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        User user = resetToken.getUser();
        SecurityAccount account = getSecurityAccountByUserOrThrow(user);

        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        securityAccountRepository.save(account);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        revokeAllRefreshTokens(user);

        log.info("Password successfully reset for user {}", user.getEmail());
    }

    @Override
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void logoutAll(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        revokeAllRefreshTokens(user);
    }

    @Override
    public AuthMeResponse me(SecurityUser securityUser) {
        return new AuthMeResponse(
                securityUser.getId(),
                securityUser.getEmail(),
                securityUser.getRole().name(),
                securityUser.getStatus().name());
    }

    @Override
    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistException("Email " + normalizedEmail + " is already in use");
        }

        invitationTokenRepository.findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .ifPresent(existingInvitation -> {
            if (existingInvitation.isValid()) {
                existingInvitation.setUsed(true);
                invitationTokenRepository.save(existingInvitation);
            }
        });

        InvitationToken invitationToken = InvitationToken.builder()
                .token(UUID.randomUUID().toString())
                .email(normalizedEmail)
                .role(request.role())
                .used(false)
                .expiresAt(Instant.now().plus(INVITATION_DAYS, ChronoUnit.DAYS))
                .build();

        InvitationToken savedToken = invitationTokenRepository.save(invitationToken);
        emailService.sendInvitationEmail(savedToken.getEmail(), savedToken.getToken());

        log.info("Invitation created for email={} with role={}", savedToken.getEmail(), savedToken.getRole());

        return new InvitationResponse(
                savedToken.getId(),
                savedToken.getEmail(),
                savedToken.getRole().name(),
                savedToken.getToken(),
                savedToken.getExpiresAt(),
                savedToken.isUsed());
    }

    private void validateAccountCanAuthenticate(User user, SecurityAccount account) {
        if (account.isLocked() || user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountLockedException("Account is locked");
        }

        if (!account.isEnabled() || user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedActionException("Account is disabled");
        }

        if (!account.isEmailVerified() || user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new AccountNotVerifiedException("Email verification is required before login");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is not active");
        }
    }

    private AuthResponse issueAuthTokens(User user, SecurityAccount account, boolean persistRefreshToken) {
        SecurityUser securityUser = new SecurityUser(user, account);

        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshToken = jwtService.generateRefreshToken(securityUser);

        if (persistRefreshToken) {
            RefreshToken storedRefreshToken = RefreshToken.builder()
                    .token(refreshToken)
                    .user(user)
                    .expiresAt(jwtService.getExpiration(refreshToken))
                    .revoked(false)
                    .build();

            refreshTokenRepository.save(storedRefreshToken);
        }

        return AuthResponse.from(user, accessToken, refreshToken);
    }

    private void issueEmailVerificationCode(User user) {
        emailVerificationTokenRepository.deleteByUser(user);

        String code = generateVerificationCode();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .user(user).code(code)
                        .expiryDate(Instant.now().plus(emailVerificationCodeTtlMinutes, ChronoUnit.MINUTES))
                        .used(false)
                        .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendEmailVerificationCode(user.getEmail(), code);
    }

    private void revokeAllRefreshTokens(User user) {
        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(token -> token.setRevoked(true));
    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private SecurityAccount getSecurityAccountByUserOrThrow(User user) {
        return securityAccountRepository.findByUser(user).orElseThrow(
                () -> new ResourceNotFoundException("Security account not found for user"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
    }

    private String generateVerificationCode() {
        int min = (int) Math.pow(10, VERIFICATION_CODE_DIGITS - 1);
        int max = (int) Math.pow(10, VERIFICATION_CODE_DIGITS);
        int code = ThreadLocalRandom.current().nextInt(min, max);
        return String.valueOf(code);
    }
}