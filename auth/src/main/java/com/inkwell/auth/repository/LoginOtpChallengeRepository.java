package com.inkwell.auth.repository;

import com.inkwell.auth.entity.LoginOtpChallenge;
import com.inkwell.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginOtpChallengeRepository extends JpaRepository<LoginOtpChallenge, Long> {

    void deleteByUser(User user);
}
