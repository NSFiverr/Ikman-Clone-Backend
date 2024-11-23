package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    private final PasswordEncoder passwordEncoder;

    public AdminMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Admin toEntity(AdminCreationRequest request) {
        return Admin.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .adminType(request.getAdminType())
                .permissions(request.getPermissions())
                .build();
    }

    public AdminResponse toResponse(Admin admin) {
        return AdminResponse.builder()
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .adminType(admin.getAdminType())
                .permissions(admin.getPermissions())
                .lastAccessAt(admin.getLastAccessAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}