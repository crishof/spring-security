package com.crishof.springsecurity.security;

import com.crishof.springsecurity.dto.AuthResponse;
import com.crishof.springsecurity.dto.InvitationResponse;
import com.crishof.springsecurity.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void me_whenAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_whenAnonymous_shouldBePublicAndReturn200() throws Exception {
        AuthResponse response = new AuthResponse(
                UUID.randomUUID(),
                "Test User",
                "user@example.com",
                "USER",
                "ACTIVE",
                "access-token",
                "refresh-token",
                "Bearer"
        );

        when(authService.login(any())).thenReturn(response);

        String payload = """
                {
                  "email": "user@example.com",
                  "password": "Passw0rd!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminInvitation_whenUserRole_shouldReturn403() throws Exception {
        String payload = """
                {
                  "email": "invitee@example.com",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        verify(authService, never()).createInvitation(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminInvitation_whenAdminRole_shouldReturn201() throws Exception {
        InvitationResponse response = new InvitationResponse(
                UUID.randomUUID(),
                "invitee@example.com",
                "USER",
                "invite-token",
                Instant.now().plusSeconds(3600),
                false
        );

        when(authService.createInvitation(any())).thenReturn(response);

        String payload = """
                {
                  "email": "invitee@example.com",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}



