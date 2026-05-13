package com.inkwell.auth.service;

import com.inkwell.auth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse oauth2Login(String registrationId, java.util.Map<String, Object> attributes);

    void logout(String refreshToken);

    ProfileResponse getProfile(String email);

    ProfileResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    void deactivateAccount(String email);
}
