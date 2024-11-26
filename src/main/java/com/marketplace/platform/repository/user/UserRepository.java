package com.marketplace.platform.repository.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Basic queries with status check
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status <> :deletedStatus")
    Optional<User> findByEmail(@Param("email") String email, @Param("deletedStatus") UserStatus deletedStatus);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.status <> :deletedStatus")
    boolean existsByEmail(@Param("email") String email, @Param("deletedStatus") UserStatus deletedStatus);

    // Optimized search with status check and fetch join
    @Query("""
        SELECT DISTINCT u FROM User u 
        LEFT JOIN FETCH u.userRoles
        WHERE (:term IS NULL OR 
              LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))
        AND (:status IS NULL OR u.status = :status)
        AND (CAST(:startDate AS timestamp) IS NULL OR u.createdAt >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR u.createdAt <= :endDate)
        AND u.status <> :deletedStatus
        """)
    Page<User> searchUsers(
            @Param("term") String term,
            @Param("status") UserStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("deletedStatus") UserStatus deletedStatus,
            Pageable pageable
    );

    // Verification and status queries
    @Query("SELECT u FROM User u WHERE u.isEmailVerified = :verified AND u.status <> :deletedStatus")
    List<User> findByIsEmailVerified(@Param("verified") Boolean verified, @Param("deletedStatus") UserStatus deletedStatus);

    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findByStatus(@Param("status") UserStatus status);

    // Activity queries
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :date AND u.status <> :excludeStatus")
    Page<User> findByLastLoginAtBefore(
            @Param("date") LocalDateTime date,
            @Param("excludeStatus") UserStatus excludeStatus,
            Pageable pageable
    );

    // Role-based queries with fetch join
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userRoles r WHERE r.roleName = :roleName AND u.status <> :deletedStatus")
    List<User> findByRole(@Param("roleName") String roleName, @Param("deletedStatus") UserStatus deletedStatus);

    // Advertisement-based queries
    @Query("SELECT u FROM User u WHERE SIZE(u.advertisements) >= :minCount AND u.status <> :deletedStatus")
    List<User> findUsersWithMinimumAds(@Param("minCount") int minCount, @Param("deletedStatus") UserStatus deletedStatus);

    // Status update operations
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status, u.updatedAt = CURRENT_TIMESTAMP WHERE u.userId = :userId AND u.status <> :deletedStatus")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") UserStatus status, @Param("deletedStatus") UserStatus deletedStatus);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = CURRENT_TIMESTAMP, u.updatedAt = CURRENT_TIMESTAMP WHERE u.userId = :userId AND u.status <> :deletedStatus")
    int updateLastLogin(@Param("userId") Long userId, @Param("deletedStatus") UserStatus deletedStatus);

    // Favorites queries with optimization
    @Query("""
        SELECT DISTINCT u FROM User u 
        LEFT JOIN FETCH u.userRoles 
        JOIN u.favorites f 
        WHERE f.advertisement.adId = :adId AND u.status <> :deletedStatus
        """)
    List<User> findUsersByFavoritedAd(@Param("adId") Long adId, @Param("deletedStatus") UserStatus deletedStatus);

    // Bulk operations
    @Query("SELECT u FROM User u WHERE u.userId IN :userIds AND u.status <> :deletedStatus")
    List<User> findByUserIdIn(@Param("userIds") Set<Long> userIds, @Param("deletedStatus") UserStatus deletedStatus);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);

    // Category-related queries
    @Query("SELECT DISTINCT u FROM User u WHERE SIZE(u.createdCategories) > 0 AND u.status <> :deletedStatus")
    List<User> findUsersWhoCreatedCategories(@Param("deletedStatus") UserStatus deletedStatus);

    // Activity and notification queries
    @Query("""
        SELECT DISTINCT u FROM User u 
        WHERE u.status <> :deletedStatus 
        AND (u.lastLoginAt >= :date 
        OR EXISTS (
            SELECT 1 FROM Advertisement a 
            WHERE a.user = u AND a.createdAt >= :date
        ))
        """)
    List<User> findUsersWithRecentActivity(@Param("date") LocalDateTime date, @Param("deletedStatus") UserStatus deletedStatus);

    @Query("SELECT DISTINCT u FROM User u JOIN u.notifications n " +
            "WHERE n.isRead = false AND u.status <> :excludeStatus")
    Page<User> findUsersWithUnreadNotifications(
            @Param("excludeStatus") UserStatus excludeStatus,
            Pageable pageable
    );

    // Phone number search
    @Query("SELECT u FROM User u WHERE u.phone LIKE :phonePattern AND u.status <> :deletedStatus")
    List<User> findByPhoneLike(@Param("phonePattern") String phonePattern, @Param("deletedStatus") UserStatus deletedStatus);

    // Soft delete for unverified users
    @Modifying
    @Transactional
    @Query("""
        UPDATE User u 
        SET u.status = :deletedStatus, 
            u.email = CONCAT(u.email, '_deleted_', u.userId),
            u.updatedAt = CURRENT_TIMESTAMP 
        WHERE u.isEmailVerified = false 
        AND u.createdAt < :date 
        AND u.status <> :deletedStatus
        """)
    int softDeleteUnverifiedUsersOlderThan(@Param("date") LocalDateTime date, @Param("deletedStatus") UserStatus deletedStatus);


    @Query("""
        SELECT COUNT(DISTINCT u) FROM User u 
        WHERE u.status <> :deletedStatus 
        AND u.lastLoginAt >= :since
        """)
    long countActiveUsersSince(@Param("since") LocalDateTime since, @Param("deletedStatus") UserStatus deletedStatus);

    @Query("""
        SELECT DISTINCT u FROM User u 
        WHERE u.status <> :deletedStatus 
        AND NOT EXISTS (
            SELECT 1 FROM Advertisement a 
            WHERE a.user = u AND a.status = 'ACTIVE'
        )
        """)
    List<User> findUsersWithNoActiveAds(@Param("deletedStatus") UserStatus deletedStatus);
}