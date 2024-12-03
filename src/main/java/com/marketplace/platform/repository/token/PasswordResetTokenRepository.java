package com.marketplace.platform.repository.token;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.AccountType;
import com.marketplace.platform.domain.token.PasswordResetToken;
import com.marketplace.platform.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    // Basic token operations
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    boolean existsByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    // User related queries
    List<PasswordResetToken> findByUser(User user);
    List<PasswordResetToken> findByAdmin(Admin admin);
    List<PasswordResetToken> findByUserAndExpiresAtAfter(User user, LocalDateTime now);
    List<PasswordResetToken> findByAdminAndExpiresAtAfter(Admin admin, LocalDateTime now);

    // Invalidate existing tokens
    @Modifying
    @Query("""
        UPDATE PasswordResetToken t 
        SET t.usedAt = CURRENT_TIMESTAMP 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    int invalidateExistingUserTokens(@Param("user") User user);

    @Modifying
    @Query("""
        UPDATE PasswordResetToken t 
        SET t.usedAt = CURRENT_TIMESTAMP 
        WHERE t.admin = :admin 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    int invalidateExistingAdminTokens(@Param("admin") Admin admin);

    // Delete expired tokens
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :datetime")
    int deleteExpiredTokens(@Param("datetime") LocalDateTime datetime);

    // Delete used tokens
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.usedAt IS NOT NULL")
    int deleteUsedTokens();

    // Find valid token for user or admin
    @Query("""
        SELECT t FROM PasswordResetToken t 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP 
        ORDER BY t.createdAt DESC
        """)
    Optional<PasswordResetToken> findValidTokenForUser(@Param("user") User user);

    @Query("""
        SELECT t FROM PasswordResetToken t 
        WHERE t.admin = :admin 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP 
        ORDER BY t.createdAt DESC
        """)
    Optional<PasswordResetToken> findValidTokenForAdmin(@Param("admin") Admin admin);

    // Count active tokens
    @Query("""
        SELECT COUNT(t) FROM PasswordResetToken t 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    long countActiveTokensForUser(@Param("user") User user);

    @Query("""
        SELECT COUNT(t) FROM PasswordResetToken t 
        WHERE t.admin = :admin 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    long countActiveTokensForAdmin(@Param("admin") Admin admin);

    // Mark token as used
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.token = :token")
    int markTokenAsUsed(@Param("token") String token);

    // Find tokens about to expire
    @Query("""
        SELECT t FROM PasswordResetToken t 
        WHERE t.expiresAt BETWEEN :start AND :end 
        AND t.usedAt IS NULL
        """)
    List<PasswordResetToken> findTokensAboutToExpire(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Delete all tokens for user or admin
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    int deleteAllUserTokens(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.admin = :admin")
    int deleteAllAdminTokens(@Param("admin") Admin admin);

    // Check if token is valid
    @Query("""
        SELECT CASE 
            WHEN COUNT(t) > 0 THEN true 
            ELSE false 
        END 
        FROM PasswordResetToken t 
        WHERE t.token = :token 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    boolean isTokenValid(@Param("token") String token);

    // Find tokens by account type
    @Query("""
        SELECT t FROM PasswordResetToken t 
        WHERE t.accountType = :accountType 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    List<PasswordResetToken> findActiveTokensByAccountType(@Param("accountType") AccountType accountType);

    // Find tokens by email (for either user or admin)
    @Query("""
        SELECT t FROM PasswordResetToken t 
        WHERE (t.user.email = :email OR t.admin.email = :email) 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    List<PasswordResetToken> findActiveTokensByEmail(@Param("email") String email);
}