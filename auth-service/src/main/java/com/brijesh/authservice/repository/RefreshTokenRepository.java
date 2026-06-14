package com.brijesh.authservice.repository;

import com.brijesh.authservice.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revoked = true, rt.revokedAt = :revokedAt
        WHERE rt.user.id = :userId AND rt.revoked = false
    """)
    void revokedAllByUserId(@Param("userId") Long userId, @Param("revokedAt")Instant revokedAt);
}
