package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.audit.AdminAuditLog;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.response.AdminAuditLogResponse;
import com.marketplace.platform.dto.response.AdminResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    private final PasswordEncoder passwordEncoder;

    public AdminMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Admin toEntity(AdminCreationRequest request, String passwordHash) {
        return Admin.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordHash)
                .role(request.getRole())
                .permissions(request.getPermissions())
                .isPasswordChanged(false)
                .build();
    }

    public AdminResponse toResponse(Admin admin) {
        return AdminResponse.builder()
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .role(admin.getRole())
                .permissions(admin.getPermissions())
                .lastAccessAt(admin.getLastAccessAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }

    public AdminAuditLog toAuditLog(Admin admin, String action, Long performedBy, String reason) {
        return AdminAuditLog.builder()
                .action(action)
                .adminId(admin.getAdminId())
                .performedBy(performedBy)
                .reason(reason)
                .details(toAuditDetails(admin))
                .build();
    }

    private String toAuditDetails(Admin admin) {
        return String.format(
                "Admin[id=%d, email=%s, type=%s, permissions=%s, createdAt=%s]",
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole(),
                admin.getPermissions(),
                admin.getCreatedAt()
        );
    }

    public AdminAuditLogResponse toAuditLogResponse(AdminAuditLog auditLog) {
        return AdminAuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .adminId(auditLog.getAdminId())
                .performedBy(auditLog.getPerformedBy())
                .reason(auditLog.getReason())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }



}