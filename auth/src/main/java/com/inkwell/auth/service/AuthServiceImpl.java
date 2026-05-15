package com.inkwell.auth.service;

import com.inkwell.auth.dto.*;
import com.inkwell.auth.entity.*;
import com.inkwell.auth.repository.LoginOtpChallengeRepository;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.exception.UnauthorizedException;
import com.inkwell.auth.repository.RefreshTokenRepository;
import com.inkwell.auth.repository.UserRepository;
import com.inkwell.auth.security.JwtService;
import com.inkwell.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final int MAX_AVATAR_URL_LENGTH = 2048;
    private final UserRepository userRepository;
    private final LoginOtpChallengeRepository loginOtpChallengeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final int LOGIN_OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_LOGIN_OTP_ATTEMPTS = 5;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user, "User registered successfully");
    }

    @Override
    public LoginOtpChallengeResponse requestLoginOtp(LoginRequest request) {
        User user = findLoginUser(request.getEmailOrUsername());

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        loginOtpChallengeRepository.deleteByUser(user);

        String otp = generateLoginOtp();
        LoginOtpChallenge challenge = LoginOtpChallenge.builder()
                .user(user)
                .otpHash(passwordEncoder.encode(otp))
                .loginIdentifier(request.getEmailOrUsername().trim())
                .expiresAt(LocalDateTime.now().plusMinutes(LOGIN_OTP_VALIDITY_MINUTES))
                .failedAttempts(0)
                .build();

        LoginOtpChallenge savedChallenge = loginOtpChallengeRepository.save(challenge);
        sendLoginOtpEmail(user, otp, savedChallenge.getExpiresAt());

        return LoginOtpChallengeResponse.builder()
                .challengeId(savedChallenge.getChallengeId())
                .email(user.getEmail())
                .maskedEmail(maskEmail(user.getEmail()))
                .expiresAt(savedChallenge.getExpiresAt())
                .message("OTP sent to your registered email")
                .build();
    }

    @Override
    public AuthResponse verifyLoginOtp(VerifyLoginOtpRequest request) {
        LoginOtpChallenge challenge = loginOtpChallengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new UnauthorizedException("OTP session is invalid or expired"));

        if (challenge.isConsumed() || challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("OTP session is invalid or expired");
        }

        User user = challenge.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getOtp(), challenge.getOtpHash())) {
            int attempts = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(attempts);
            if (attempts >= MAX_LOGIN_OTP_ATTEMPTS) {
                challenge.setConsumed(true);
                challenge.setConsumedAt(LocalDateTime.now());
            }
            loginOtpChallengeRepository.save(challenge);
            throw new UnauthorizedException(attempts >= MAX_LOGIN_OTP_ATTEMPTS
                    ? "OTP verification failed too many times. Please request a new code"
                    : "Invalid OTP");
        }

        challenge.setConsumed(true);
        challenge.setConsumedAt(LocalDateTime.now());
        loginOtpChallengeRepository.save(challenge);

        return buildAuthResponse(user, "Login successful");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = findLoginUser(request.getEmailOrUsername());

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        return buildAuthResponse(user, "Login successful");
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return buildAuthResponse(user, "Token refreshed successfully");
    }

    @Override
    public AuthResponse oauth2Login(String registrationId, Map<String, Object> attributes) {
        AuthProvider provider = resolveProvider(registrationId);
        String email = extractEmail(provider, attributes);
        String username = extractUsername(provider, attributes, email);
        String fullName = extractFullName(provider, attributes, username);
        String avatarUrl = extractAvatarUrl(provider, attributes);

        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateOAuth2User(existingUser, provider, fullName, avatarUrl))
                .orElseGet(() -> createOAuth2User(email, username, fullName, avatarUrl, provider));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return buildAuthResponse(user, "OAuth2 login successful");
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToProfileResponse(user);
    }

    @Override
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeAvatarUrl(request.getAvatarUrl()));
        }

        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
    }

    private User createOAuth2User(String email, String username, String fullName, String avatarUrl, AuthProvider provider) {
        User user = User.builder()
                .username(generateUniqueUsername(username))
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName)
                .role(Role.READER)
                .avatarUrl(normalizeAvatarUrl(avatarUrl))
                .provider(provider)
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    private User updateOAuth2User(User user, AuthProvider provider, String fullName, String avatarUrl) {
        if (user.getProvider() != provider) {
            user.setProvider(provider);
        }
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
        }
        return userRepository.save(user);
    }

    private User findLoginUser(String emailOrUsername) {
        String normalizedEmail = emailOrUsername.trim().toLowerCase();
        String normalizedUsername = emailOrUsername.trim();
        return userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByUsername(normalizedUsername))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
    }

    private AuthProvider resolveProvider(String registrationId) {
        try {
            return AuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Unsupported OAuth2 provider");
        }
    }

    private String extractEmail(AuthProvider provider, Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email == null || email.toString().isBlank()) {
            throw new UnauthorizedException(provider.name() + " account does not expose an email address");
        }
        return email.toString().trim().toLowerCase();
    }

    private String extractUsername(AuthProvider provider, Map<String, Object> attributes, String email) {
        Object username = provider == AuthProvider.GITHUB ? attributes.get("login") : attributes.get("name");
        if (username == null || username.toString().isBlank()) {
            username = email.substring(0, email.indexOf("@"));
        }
        return username.toString().trim().toLowerCase().replaceAll("[^a-z0-9._-]", "");
    }

    private String extractFullName(AuthProvider provider, Map<String, Object> attributes, String fallbackUsername) {
        Object fullName = provider == AuthProvider.GITHUB ? attributes.get("name") : attributes.get("name");
        if (fullName == null || fullName.toString().isBlank()) {
            return fallbackUsername;
        }
        return fullName.toString().trim();
    }

    private String extractAvatarUrl(AuthProvider provider, Map<String, Object> attributes) {
        Object avatarUrl = provider == AuthProvider.GITHUB ? attributes.get("avatar_url") : attributes.get("picture");
        return avatarUrl == null ? null : avatarUrl.toString();
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }

        String normalized = avatarUrl.trim();
        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > MAX_AVATAR_URL_LENGTH) {
            return normalized.substring(0, MAX_AVATAR_URL_LENGTH);
        }

        return normalized;
    }

    private String generateUniqueUsername(String baseUsername) {
        String sanitized = baseUsername;
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "user";
        }
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }

        String candidate = sanitized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixText = "-" + suffix++;
            int maxBaseLength = 50 - suffixText.length();
            String truncatedBase = sanitized.length() > maxBaseLength
                    ? sanitized.substring(0, maxBaseLength)
                    : sanitized;
            candidate = truncatedBase + suffixText;
        }
        return candidate;
    }

    private String generateLoginOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private void sendLoginOtpEmail(User user, String otp, LocalDateTime expiresAt) {
        validateMailConfiguration();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Email service is not configured. No mail sender bean is available.");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailUsername);
        email.setTo(user.getEmail());
        email.setSubject("Your InkWell login OTP");
        email.setText("""
                Hello %s,

                Your InkWell login OTP is: %s

                This code will expire at %s and can be used only once.

                If you did not try to sign in, please ignore this email.
                """.formatted(user.getFullName(), otp, expiresAt));
        try {
            mailSender.send(email);
        } catch (MailException ex) {
            throw new IllegalStateException(resolveMailErrorMessage(ex));
        }
    }

    private void validateMailConfiguration() {
        if (mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank()
                || "your_email@gmail.com".equalsIgnoreCase(mailUsername)
                || "your_app_password".equals(mailPassword)) {
            throw new IllegalStateException("Email service is not configured. Set real spring.mail.username and spring.mail.password values.");
        }
    }

    private String resolveMailErrorMessage(MailException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return "Email delivery failed: " + cause.getMessage();
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return "Email delivery failed: " + ex.getMessage();
        }
        return "Email delivery failed";
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        }
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1) + domain;
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        refreshTokenRepository.deleteByUser(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .message(message)
                .build();
    }

    private ProfileResponse mapToProfileResponse(User user) {
        return ProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
