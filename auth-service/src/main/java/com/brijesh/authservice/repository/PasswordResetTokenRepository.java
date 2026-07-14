package com.brijesh.authservice.repository;

import com.brijesh.authservice.domain.entity.PasswordResetToken;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findPasswordResetTokenByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE PasswordResetToken t
        SET t.used = true
        WHERE t.user.id = :userId AND t.used = false
    """)
    void markAllAsUsedByUserId(@Param("userId") Long userId);
}
