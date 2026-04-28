package com.inkwell.auth.repository;

import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.RoleChangeRequest;
import com.inkwell.auth.entity.RoleRequestStatus;
import com.inkwell.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, Long> {
    boolean existsByUserAndRequestedRoleAndStatus(User user, Role requestedRole, RoleRequestStatus status);

    @EntityGraph(attributePaths = "user")
    List<RoleChangeRequest> findByUserOrderByRequestedAtDesc(User user);

    @EntityGraph(attributePaths = "user")
    List<RoleChangeRequest> findAllByOrderByRequestedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<RoleChangeRequest> findByStatusOrderByRequestedAtDesc(RoleRequestStatus status);
}
