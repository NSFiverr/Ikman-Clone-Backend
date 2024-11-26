package com.marketplace.platform.repository.token;

import com.marketplace.platform.domain.token.VerificationToken;
import com.marketplace.platform.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    // Basic token operations
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    boolean existsByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    // User related queries
    List<VerificationToken> findByUser(User user);
    List<VerificationToken> findByUserAndExpiresAtAfter(User user, LocalDateTime now);

    // Find valid token
    @Query("""
        SELECT t FROM VerificationToken t 
        WHERE t.token = :token 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    Optional<VerificationToken> findValidToken(@Param("token") String token);

    // Invalidate existing tokens for user
    @Modifying
    @Query("""
        UPDATE VerificationToken t 
        SET t.usedAt = CURRENT_TIMESTAMP 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    int invalidateExistingTokens(@Param("user") User user);

    // Delete expired tokens
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :datetime")
    int deleteExpiredTokens(@Param("datetime") LocalDateTime datetime);

    // Delete used tokens
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.usedAt IS NOT NULL")
    int deleteUsedTokens();

    // Find latest valid token for user
    @Query("""
        SELECT t FROM VerificationToken t 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP 
        ORDER BY t.createdAt DESC
        """)
    Optional<VerificationToken> findLatestValidTokenForUser(@Param("user") User user);

    // Count active tokens for user
    @Query("""
        SELECT COUNT(t) FROM VerificationToken t 
        WHERE t.user = :user 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    long countActiveTokensForUser(@Param("user") User user);

    // Mark token as used
    @Modifying
    @Query("UPDATE VerificationToken t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.token = :token")
    int markTokenAsUsed(@Param("token") String token);

    // Find tokens about to expire
    @Query("""
        SELECT t FROM VerificationToken t 
        WHERE t.expiresAt BETWEEN :start AND :end 
        AND t.usedAt IS NULL
        """)
    List<VerificationToken> findTokensAboutToExpire(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Delete all tokens for user
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user = :user")
    int deleteAllUserTokens(@Param("user") User user);

    // Check if token is valid
    @Query("""
        SELECT CASE 
            WHEN COUNT(t) > 0 THEN true 
            ELSE false 
        END 
        FROM VerificationToken t 
        WHERE t.token = :token 
        AND t.usedAt IS NULL 
        AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    boolean isTokenValid(@Param("token") String token);

    // Find unused expired tokens
    @Query("""
        SELECT t FROM VerificationToken t 
        WHERE t.expiresAt < CURRENT_TIMESTAMP 
        AND t.usedAt IS NULL
        """)
    List<VerificationToken> findUnusedExpiredTokens();

    // Get token statistics for user
    @Query("""
        SELECT new map(
            COUNT(t) as totalTokens,
            COUNT(CASE WHEN t.usedAt IS NOT NULL THEN 1 END) as usedTokens,
            COUNT(CASE WHEN t.expiresAt < CURRENT_TIMESTAMP THEN 1 END) as expiredTokens,
            COUNT(CASE WHEN t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP THEN 1 END) as activeTokens
        )
        FROM VerificationToken t
        WHERE t.user = :user
        """)
    Map<String, Long> getTokenStatisticsForUser(@Param("user") User user);
}