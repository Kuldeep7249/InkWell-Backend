package com.inkwell.auth.repository;

import com.inkwell.auth.entity.Role;
import com.inkwell.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(Role role);

    List<User> findByUsernameContainingIgnoreCase(String username);

    void deleteByUserId(Long userId);
}
