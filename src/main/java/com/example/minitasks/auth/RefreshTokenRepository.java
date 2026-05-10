package com.example.minitasks.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken r where r.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
