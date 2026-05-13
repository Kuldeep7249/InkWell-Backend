package com.inkwell.auth.security;

import com.inkwell.auth.entity.AuthProvider;
import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 60_000L);

        User user = User.builder()
                .userId(99L)
                .username("admin")
                .email("admin@example.com")
                .passwordHash("hash")
                .fullName("Admin")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        principal = new UserPrincipal(user);
    }

    @Test
    void generatesTokenWithSubjectAndValidatesIt() {
        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }
}
