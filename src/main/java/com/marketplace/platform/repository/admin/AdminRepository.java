package com.marketplace.platform.repository.admin;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {

    // Basic CRUD operations are automatically provided by JpaRepository

    // Find admin by email
    Optional<Admin> findByEmail(String email);

    // Find admins by type
    List<Admin> findByRole(Role role);

    // Find admins by type and sorted by creation date
    List<Admin> findByRoleOrderByCreatedAtDesc(Role role);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find admins who haven't accessed the system since a specific date
    List<Admin> findByLastAccessAtBefore(LocalDateTime date);

    // Find admins created between two dates
    List<Admin> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Update last access time for an admin
    @Modifying
    @Query("UPDATE Admin a SET a.lastAccessAt = :lastAccessAt WHERE a.adminId = :adminId")
    void updateLastAccessAt(@Param("adminId") Long adminId, @Param("lastAccessAt") LocalDateTime lastAccessAt);

    // Update admin permissions
    @Modifying
    @Query("UPDATE Admin a SET a.permissions = :permissions WHERE a.adminId = :adminId")
    void updatePermissions(@Param("adminId") Long adminId, @Param("permissions") String permissions);

    // Delete admins who haven't accessed the system for a specific period
    @Modifying
    @Query("DELETE FROM Admin a WHERE a.lastAccessAt < :date")
    void deleteInactiveAdmins(@Param("date") LocalDateTime date);

    // Find admins with specific permissions
    @Query("SELECT a FROM Admin a WHERE a.permissions LIKE %:permission%")
    List<Admin> findByPermissionContaining(@Param("permission") String permission);

    // Count admins by type
    long countByRole(Role role);
}