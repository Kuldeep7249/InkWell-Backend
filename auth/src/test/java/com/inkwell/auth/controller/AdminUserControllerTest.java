package com.inkwell.auth.controller;

import com.inkwell.auth.dto.ProfileResponse;
import com.inkwell.auth.dto.SendNotificationRequest;
import com.inkwell.auth.dto.UpdateUserBlockRequest;
import com.inkwell.auth.entity.AuthProvider;
import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.User;
import com.inkwell.auth.messaging.NotificationEventPublisher;
import com.inkwell.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminUserController adminUserController;

    private User managedUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        managedUser = User.builder()
                .userId(10L)
                .username("reader")
                .email("reader@example.com")
                .passwordHash("hash")
                .fullName("Reader One")
                .role(Role.READER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .userId(1L)
                .username("admin")
                .email("admin@example.com")
                .passwordHash("hash")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

    }

    @Test
    void blockAndUnblockEndpointsToggleActiveState() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUserId(10L)).thenReturn(Optional.of(managedUser));

        ProfileResponse blocked = adminUserController.blockUser(10L, authentication);
        assertThat(blocked.isActive()).isFalse();
        assertThat(managedUser.isActive()).isFalse();

        ProfileResponse unblocked = adminUserController.unblockUser(10L, authentication);
        assertThat(unblocked.isActive()).isTrue();
        assertThat(managedUser.isActive()).isTrue();
    }

    @Test
    void genericUpdateSupportsFrontendPayloadVariants() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUserId(10L)).thenReturn(Optional.of(managedUser));

        UpdateUserBlockRequest blockedRequest = new UpdateUserBlockRequest();
        blockedRequest.setIsBlocked(true);
        ProfileResponse blocked = adminUserController.updateUser(10L, blockedRequest, authentication);
        assertThat(blocked.isActive()).isFalse();

        UpdateUserBlockRequest activeRequest = new UpdateUserBlockRequest();
        activeRequest.setActive(true);
        ProfileResponse unblocked = adminUserController.patchUser(10L, activeRequest, authentication);
        assertThat(unblocked.isActive()).isTrue();

        UpdateUserBlockRequest statusRequest = new UpdateUserBlockRequest();
        statusRequest.setStatus("BLOCKED");
        adminUserController.updateUserStatus(10L, statusRequest, authentication);

        ArgumentCaptor<SendNotificationRequest> notificationCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationEventPublisher, org.mockito.Mockito.atLeastOnce()).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues()).isNotEmpty();
    }

    @Test
    void genericUpdateRejectsMissingBlockState() {
        UpdateUserBlockRequest invalidRequest = new UpdateUserBlockRequest();

        assertThatThrownBy(() -> adminUserController.updateUser(10L, invalidRequest, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Block status is required");
    }
}
