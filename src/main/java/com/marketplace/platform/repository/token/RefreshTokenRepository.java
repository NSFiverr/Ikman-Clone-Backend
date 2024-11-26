package com.marketplace.platform.repository.token;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByAdminAndRevokedAtIsNull(Admin admin);

    List<RefreshToken> findByUserAndRevokedAtIsNull(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = ?2, r.revokedBy = ?3, r.revocationReason = ?4 WHERE r.admin = ?1 AND r.revokedAt IS NULL")
    void revokeAllAdminTokens(Admin admin, LocalDateTime revokedAt, String revokedBy, String reason);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = ?2, r.revokedBy = ?3, r.revocationReason = ?4 WHERE r.user = ?1 AND r.revokedAt IS NULL")
    void revokeAllUserTokens(User user, LocalDateTime revokedAt, String revokedBy, String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < ?1")
    void deleteAllExpiredTokens(LocalDateTime now);
}