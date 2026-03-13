package com.crishof.springsecurity.service;


import com.crishof.springsecurity.dto.*;
import com.crishof.springsecurity.security.principal.SecurityUser;

import java.util.UUID;

public interface AuthService {

    SignupResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    AuthResponse acceptInvite(AcceptInviteRequest request);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void logout(String refreshToken);

    void logoutAll(UUID userId);

    AuthMeResponse me(SecurityUser securityUser);

    InvitationResponse createInvitation(CreateInvitationRequest request);
}
