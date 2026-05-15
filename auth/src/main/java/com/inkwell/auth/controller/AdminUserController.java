package com.inkwell.auth.controller;

import com.inkwell.auth.dto.NotificationType;
import com.inkwell.auth.dto.ProfileResponse;
import com.inkwell.auth.dto.RelatedType;
import com.inkwell.auth.dto.SendNotificationRequest;
import com.inkwell.auth.dto.UpdateUserBlockRequest;
import com.inkwell.auth.dto.UpdateUserRoleRequest;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.exception.ResourceNotFoundException;
import com.inkwell.auth.messaging.NotificationEventPublisher;
import com.inkwell.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @GetMapping
    public List<ProfileResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    @PutMapping("/{userId}/role")
    public ProfileResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            Authentication authentication
    ) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(request.getRole());
        User savedUser = userRepository.save(user);
        Long actorId = userRepository.findByEmail(authentication.getName()).map(User::getUserId).orElse(null);
        sendRoleNotification(savedUser, actorId, "Role updated", "Your role has been updated to " + savedUser.getRole().name() + ".");

        return mapToProfileResponse(savedUser);
    }

    @PutMapping("/{userId}/block")
    public ProfileResponse blockUser(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, true, authentication);
    }

    @PatchMapping("/{userId}/block")
    public ProfileResponse patchBlockUser(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, true, authentication);
    }

    @PutMapping("/block/{userId}")
    public ProfileResponse blockUserAlternative(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, true, authentication);
    }

    @PatchMapping("/block/{userId}")
    public ProfileResponse patchBlockUserAlternative(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, true, authentication);
    }

    @PutMapping("/{userId}/unblock")
    public ProfileResponse unblockUser(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, false, authentication);
    }

    @PatchMapping("/{userId}/unblock")
    public ProfileResponse patchUnblockUser(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, false, authentication);
    }

    @PutMapping("/unblock/{userId}")
    public ProfileResponse unblockUserAlternative(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, false, authentication);
    }

    @PatchMapping("/unblock/{userId}")
    public ProfileResponse patchUnblockUserAlternative(@PathVariable Long userId, Authentication authentication) {
        return updateBlockedState(userId, false, authentication);
    }

    @PutMapping("/{userId}/status")
    public ProfileResponse updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserBlockRequest request,
            Authentication authentication
    ) {
        return updateUserBlockRequest(userId, request, authentication);
    }

    @PatchMapping("/{userId}/status")
    public ProfileResponse patchUserStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserBlockRequest request,
            Authentication authentication
    ) {
        return updateUserBlockRequest(userId, request, authentication);
    }

    @PutMapping("/{userId}")
    public ProfileResponse updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserBlockRequest request,
            Authentication authentication
    ) {
        return updateUserBlockRequest(userId, request, authentication);
    }

    @PatchMapping("/{userId}")
    public ProfileResponse patchUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserBlockRequest request,
            Authentication authentication
    ) {
        return updateUserBlockRequest(userId, request, authentication);
    }

    private void sendRoleNotification(User user, Long actorId, String title, String message) {
        notificationEventPublisher.publish(SendNotificationRequest.builder()
                .recipientId(user.getUserId())
                .actorId(actorId)
                .type(NotificationType.SYSTEM_ALERT)
                .title(title)
                .message(message)
                .relatedId(user.getUserId())
                .relatedType(RelatedType.USER)
                .sendEmail(false)
                .build());
    }

    private ProfileResponse updateUserBlockRequest(Long userId, UpdateUserBlockRequest request, Authentication authentication) {
        Boolean blocked = request == null ? null : request.resolveBlocked();
        if (blocked == null) {
            throw new IllegalArgumentException("Block status is required");
        }
        return updateBlockedState(userId, blocked, authentication);
    }

    private ProfileResponse updateBlockedState(Long userId, boolean blocked, Authentication authentication) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(!blocked);
        User savedUser = userRepository.save(user);

        Long actorId = userRepository.findByEmail(authentication.getName()).map(User::getUserId).orElse(null);
        sendRoleNotification(
                savedUser,
                actorId,
                blocked ? "Account blocked" : "Account unblocked",
                blocked ? "Your account has been blocked by an administrator." : "Your account has been unblocked by an administrator."
        );

        return mapToProfileResponse(savedUser);
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
