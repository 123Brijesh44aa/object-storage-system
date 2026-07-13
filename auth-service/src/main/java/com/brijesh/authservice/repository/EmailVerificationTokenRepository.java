package com.brijesh.authservice.repository;

import com.brijesh.authservice.domain.entity.EmailVerificationToken;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken,Long> {

    Optional<EmailVerificationToken> findEmailVerificationTokenByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE EmailVerificationToken t
        SET t.used = true
        WHERE t.user.id = :userId AND t.used = false
    """)
    void markAllAsUsedByUserId(@Param("userId") Long userId);
}
