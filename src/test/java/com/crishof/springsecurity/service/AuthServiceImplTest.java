package com.crishof.springsecurity.service;

import com.crishof.springsecurity.dto.InviteInfoResponse;
import com.crishof.springsecurity.exception.InvalidTokenException;
import com.crishof.springsecurity.model.EmailVerificationToken;
import com.crishof.springsecurity.model.InvitationToken;
import com.crishof.springsecurity.model.Role;
import com.crishof.springsecurity.model.SecurityAccount;
import com.crishof.springsecurity.model.User;
import com.crishof.springsecurity.model.UserStatus;
import com.crishof.springsecurity.repository.EmailVerificationTokenRepository;
import com.crishof.springsecurity.repository.InvitationTokenRepository;
import com.crishof.springsecurity.repository.PasswordResetTokenRepository;
import com.crishof.springsecurity.repository.RefreshTokenRepository;
import com.crishof.springsecurity.repository.SecurityAccountRepository;
import com.crishof.springsecurity.repository.UserRepository;
import com.crishof.springsecurity.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityAccountRepository securityAccountRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private InvitationTokenRepository invitationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                securityAccountRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                invitationTokenRepository,
                passwordEncoder,
                authenticationManager,
                emailService,
                jwtService
        );
        ReflectionTestUtils.setField(authService, "emailVerificationCodeTtlMinutes", 10L);
    }

    @Test
    void resendEmailVerificationCode_whenUserDoesNotExist_shouldNotIssueCode() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        authService.resendEmailVerificationCode("missing@example.com");

        verify(securityAccountRepository, never()).findByUser(any(User.class));
        verify(emailVerificationTokenRepository, never()).deleteByUser(any(User.class));
        verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
        verify(emailService, never()).sendEmailVerificationCode(any(String.class), any(String.class));
    }

    @Test
    void resendEmailVerificationCode_whenAlreadyVerified_shouldNotIssueCode() {
        User user = pendingUser("verified@example.com");
        SecurityAccount account = securityAccount(user, true, true);

        when(userRepository.findByEmailIgnoreCase("verified@example.com")).thenReturn(Optional.of(user));
        when(securityAccountRepository.findByUser(user)).thenReturn(Optional.of(account));

        authService.resendEmailVerificationCode("verified@example.com");

        verify(emailVerificationTokenRepository, never()).deleteByUser(user);
        verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
        verify(emailService, never()).sendEmailVerificationCode(any(String.class), any(String.class));
    }

    @Test
    void resendEmailVerificationCode_whenAccountDisabled_shouldNotIssueCode() {
        User user = pendingUser("disabled@example.com");
        SecurityAccount account = securityAccount(user, false, false);

        when(userRepository.findByEmailIgnoreCase("disabled@example.com")).thenReturn(Optional.of(user));
        when(securityAccountRepository.findByUser(user)).thenReturn(Optional.of(account));

        authService.resendEmailVerificationCode("disabled@example.com");

        verify(emailVerificationTokenRepository, never()).deleteByUser(user);
        verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
        verify(emailService, never()).sendEmailVerificationCode(any(String.class), any(String.class));
    }

    @Test
    void resendEmailVerificationCode_whenPendingAndEnabled_shouldIssueCodeAndSendEmail() {
        User user = pendingUser("pending@example.com");
        SecurityAccount account = securityAccount(user, false, true);

        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));
        when(securityAccountRepository.findByUser(user)).thenReturn(Optional.of(account));

        authService.resendEmailVerificationCode("pending@example.com");

        verify(emailVerificationTokenRepository).deleteByUser(user);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getCode()).hasSize(6).matches("\\d{6}");
        assertThat(savedToken.getExpiryDate()).isAfter(Instant.now().plus(9, ChronoUnit.MINUTES));
        assertThat(savedToken.getExpiryDate()).isBefore(Instant.now().plus(11, ChronoUnit.MINUTES));

        verify(emailService).sendEmailVerificationCode(eq("pending@example.com"), any(String.class));
    }

    @Test
    void getInvitationInfo_whenTokenBlank_shouldThrowInvalidTokenException() {
        assertThatThrownBy(() -> authService.getInvitationInfo(" "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invitation token is required");

        verify(invitationTokenRepository, never()).findByToken(any(String.class));
    }

    @Test
    void getInvitationInfo_whenTokenIsUnknown_shouldThrowInvalidTokenException() {
        when(invitationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getInvitationInfo("bad-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired invitation token");
    }

    @Test
    void getInvitationInfo_whenTokenAlreadyUsed_shouldThrowInvalidTokenException() {
        InvitationToken token = InvitationToken.builder()
                .token("used-token")
                .email("invitee@example.com")
                .role(Role.USER)
                .used(true)
                .expiresAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();

        when(invitationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.getInvitationInfo("used-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired invitation token");
    }

    @Test
    void getInvitationInfo_whenTokenIsValid_shouldReturnMappedResponse() {
        InvitationToken token = InvitationToken.builder()
                .id(UUID.randomUUID())
                .token("valid-token")
                .email("invitee@example.com")
                .role(Role.ADMIN)
                .used(false)
                .expiresAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();

        when(invitationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        InviteInfoResponse response = authService.getInvitationInfo("valid-token");

        assertThat(response.email()).isEqualTo("invitee@example.com");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.expiresAt()).isEqualTo(token.getExpiresAt());
        assertThat(response.passwordRequirements())
                .isEqualTo("Password must be 8-72 characters with uppercase, lowercase, digit and special character");
    }

    private User pendingUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        return user;
    }

    private SecurityAccount securityAccount(User user, boolean emailVerified, boolean enabled) {
        return SecurityAccount.builder()
                .user(user)
                .passwordHash("hash")
                .emailVerified(emailVerified)
                .enabled(enabled)
                .locked(false)
                .build();
    }
}



