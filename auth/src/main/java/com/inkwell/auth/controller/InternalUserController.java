package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ProfileResponse;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ProfileResponse getUserById(@PathVariable Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
