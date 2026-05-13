package com.inkwell.auth.service;

import com.inkwell.auth.dto.AuthResponse;
import com.inkwell.auth.dto.ChangePasswordRequest;
import com.inkwell.auth.dto.LoginRequest;
import com.inkwell.auth.dto.ProfileResponse;
import com.inkwell.auth.dto.RegisterRequest;
import com.inkwell.auth.dto.UpdateProfileRequest;
import com.inkwell.auth.entity.AuthProvider;
import com.inkwell.auth.entity.RefreshToken;
import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.exception.UnauthorizedException;
import com.inkwell.auth.repository.RefreshTokenRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId(7L)
                .username("reader")
                .email("reader@example.com")
                .passwordHash("hash")
                .fullName("Reader One")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerCreatesReaderAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(" reader ");
        request.setEmail("READER@EXAMPLE.COM ");
        request.setPassword("secret123");
        request.setFullName(" Reader One ");

        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(7L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("reader@example.com");
        assertThat(response.getRole()).isEqualTo(Role.READER);
        assertThat(response.getMessage()).isEqualTo("User registered successfully");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("reader");
        assertThat(userCaptor.getValue().isActive()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registerRejectsDuplicateEmailAndUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("reader");
        request.setEmail("reader@example.com");
        request.setPassword("secret123");
        request.setFullName("Reader One");

        when(userRepository.existsByEmail("reader@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists");

        when(userRepository.existsByEmail("reader@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("reader")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void loginAuthenticatesActiveUserAndRejectsInvalidOrInactiveUsers() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("reader@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        verify(authenticationManager).authenticate(any());

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("missing@example.com")).thenReturn(Optional.empty());
        LoginRequest missing = new LoginRequest();
        missing.setEmailOrUsername("missing@example.com");
        missing.setPassword("secret123");
        assertThatThrownBy(() -> authService.login(missing))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        activeUser.setActive(false);
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Account is deactivated");
    }

    @Test
    void refreshTokenRotatesValidTokenAndRejectsExpiredToken() {
        com.inkwell.auth.dto.RefreshTokenRequest request = new com.inkwell.auth.dto.RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        RefreshToken stored = RefreshToken.builder()
                .token("refresh-token")
                .user(activeUser)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(any())).thenReturn("new-access");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);

        stored.setRevoked(false);
        stored.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token expired or revoked");
    }

    @Test
    void profileUpdatePasswordChangeAndDeactivateUseCurrentUser() {
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(activeUser));

        ProfileResponse profile = authService.getProfile("reader@example.com");
        assertThat(profile.getUsername()).isEqualTo("reader");

        UpdateProfileRequest update = new UpdateProfileRequest();
        update.setFullName(" Updated Name ");
        update.setBio(" bio ");
        update.setAvatarUrl(" avatar ");
        ProfileResponse updated = authService.updateProfile("reader@example.com", update);
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getBio()).isEqualTo("bio");

        ChangePasswordRequest passwordRequest = new ChangePasswordRequest();
        passwordRequest.setCurrentPassword("old");
        passwordRequest.setNewPassword("newSecret");
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.matches("newSecret", "hash")).thenReturn(false);
        when(passwordEncoder.encode("newSecret")).thenReturn("newHash");
        authService.changePassword("reader@example.com", passwordRequest);
        assertThat(activeUser.getPasswordHash()).isEqualTo("newHash");
        verify(refreshTokenRepository).deleteByUser(activeUser);

        authService.deactivateAccount("reader@example.com");
        assertThat(activeUser.isActive()).isFalse();
    }

    @Test
    void passwordChangeRejectsWrongOrSamePasswordAndMissingProfileThrows() {
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(activeUser));
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("newSecret");

        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        assertThatThrownBy(() -> authService.changePassword("reader@example.com", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Current password is incorrect");

        request.setCurrentPassword("old");
        request.setNewPassword("old");
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        assertThatThrownBy(() -> authService.changePassword("reader@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password must be different from current password");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getProfile("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void oauth2LoginCreatesAndUpdatesUsersAndRejectsUnsupportedProvider() {
        when(userRepository.findByEmail("octo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });
        when(passwordEncoder.encode(any())).thenReturn("generated");
        when(jwtService.generateAccessToken(any())).thenReturn("oauth-token");

        AuthResponse created = authService.oauth2Login("github", Map.of(
                "email", "octo@example.com",
                "login", "octo",
                "name", "Octo Cat",
                "avatar_url", "https://example.com/a.png"));

        assertThat(created.getEmail()).isEqualTo("octo@example.com");
        assertThat(created.getAccessToken()).isEqualTo("oauth-token");

        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(activeUser));
        AuthResponse updated = authService.oauth2Login("google", Map.of(
                "email", "reader@example.com",
                "name", "Reader Google",
                "picture", "https://example.com/p.png"));
        assertThat(updated.getMessage()).isEqualTo("OAuth2 login successful");
        assertThat(activeUser.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(activeUser.getFullName()).isEqualTo("Reader Google");

        assertThatThrownBy(() -> authService.oauth2Login("unknown", Map.of()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unsupported OAuth2 provider");
    }
}
