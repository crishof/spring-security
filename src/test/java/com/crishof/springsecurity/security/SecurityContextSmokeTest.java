package com.crishof.springsecurity.security;

import com.crishof.springsecurity.security.config.SecurityConfig;
import com.crishof.springsecurity.security.jwt.JwtFilter;
import com.crishof.springsecurity.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityContextSmokeTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private JwtService jwtService;

    @Test
    void securityBeansLoad() {
        assertThat(securityConfig).isNotNull();
        assertThat(securityFilterChain).isNotNull();
        assertThat(jwtFilter).isNotNull();
        assertThat(jwtService).isNotNull();
    }
}

