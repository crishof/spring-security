package com.crishof.springsecurity.repository;

import com.crishof.springsecurity.model.PasswordResetToken;
import com.crishof.springsecurity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
