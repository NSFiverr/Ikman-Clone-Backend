package com.marketplace.platform.repository.user;

import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.domain.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Basic queries
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Search users with multiple criteria
    @Query("""
        SELECT u FROM User u 
        WHERE (:term IS NULL OR 
              LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR 
              LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))
        AND (:status IS NULL OR u.status = :status)
        AND (CAST(:startDate AS timestamp) IS NULL OR u.createdAt >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR u.createdAt <= :endDate)
        """)
    Page<User> searchUsers(
            @Param("term") String term,
            @Param("status") UserStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find users by verification status
    List<User> findByIsEmailVerified(Boolean isEmailVerified);

    // Find users by status
    List<User> findByStatus(UserStatus status);

    // Find users who haven't logged in since a specific date
    List<User> findByLastLoginAtBefore(LocalDateTime date);

    // Find users by role
    @Query("SELECT u FROM User u JOIN u.userRoles r WHERE r.roleName = :roleName")
    List<User> findByRole(@Param("roleName") String roleName);

    // Find users with specific advertisement counts
    @Query("SELECT u FROM User u WHERE SIZE(u.advertisements) >= :minCount")
    List<User> findUsersWithMinimumAds(@Param("minCount") int minCount);

    // Update user status
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.userId = :userId")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") UserStatus status);

    // Update last login
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    int updateLastLogin(@Param("userId") Long userId);

    // Find users who have favorited a specific advertisement
    @Query("SELECT u FROM User u JOIN u.favorites f WHERE f.advertisement.id = :adId")
    List<User> findUsersByFavoritedAd(@Param("adId") Long adId);

    // Find users by multiple IDs
    List<User> findByUserIdIn(Set<Long> userIds);

    // Count users by status
    long countByStatus(UserStatus status);

    // Find users who created categories
    @Query("SELECT DISTINCT u FROM User u WHERE SIZE(u.createdCategories) > 0")
    List<User> findUsersWhoCreatedCategories();

    // Find users with recent activity
    @Query("SELECT DISTINCT u FROM User u WHERE u.lastLoginAt >= :date OR EXISTS (SELECT 1 FROM Advertisement a WHERE a.user = u AND a.createdAt >= :date)")
    List<User> findUsersWithRecentActivity(@Param("date") LocalDateTime date);

    // Find users with unread notifications
    @Query("SELECT DISTINCT u FROM User u JOIN u.notifications n WHERE n.isRead = false")
    List<User> findUsersWithUnreadNotifications();

    // Find users by phone number pattern
    List<User> findByPhoneLike(String phonePattern);

    // Delete unverified users older than a specific date
    @Modifying
    @Query("DELETE FROM User u WHERE u.isEmailVerified = false AND u.createdAt < :date")
    int deleteUnverifiedUsersOlderThan(@Param("date") LocalDateTime date);
}