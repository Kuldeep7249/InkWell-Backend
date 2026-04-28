package com.inkwell.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleRequestStatus status;

    @Column(length = 500)
    private String reason;

    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = RoleRequestStatus.PENDING;
        if (this.requestedAt == null) this.requestedAt = LocalDateTime.now();
    }
}
