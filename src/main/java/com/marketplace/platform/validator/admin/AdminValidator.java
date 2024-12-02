package com.marketplace.platform.validator.admin;

import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.UnauthorizedOperationException;
import com.marketplace.platform.repository.admin.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminValidator {
    private static final int MAX_SUPER_ADMINS = 3;

    private final AdminRepository adminRepository;

    public void validateAdminCreation(AdminCreationRequest request) {
        // Validate email
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Admin with this email already exists");
        }

        // Validate name fields
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new BadRequestException("First name is required");
        }

        if (!StringUtils.hasText(request.getLastName())) {
            throw new BadRequestException("Last name is required");
        }

        // Validate role
        if (request.getRole() == null) {
            throw new BadRequestException("Role is required");
        }

        // Check SUPER_ADMIN limit
        if (request.getRole() == Role.SUPER_ADMIN) {
            long currentSuperAdminCount = adminRepository.countByRole(Role.SUPER_ADMIN);
            if (currentSuperAdminCount >= MAX_SUPER_ADMINS) {
                throw new BadRequestException("Maximum number of SUPER_ADMIN users (" + MAX_SUPER_ADMINS + ") has been reached");
            }
        }
    }

    public void validateAdminUpdate(Long adminId, UpdateAdminRequest request) {
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (adminRepository.existsByEmailAndAdminIdNot(request.getEmail(), adminId)) {
                throw new BadRequestException("Email is already taken");
            }
        }

//        if (request.getPassword() != null && request.getPassword().length() < 6) {
//            System.out.println(request.getPassword());
//            throw new BadRequestException("Password must be at least 6 characters long");
//        }
    }

    public void validateAdminDeletion(Long adminId, AdminDeletionRequest request, Long deletedBy) {
        if (adminId.equals(deletedBy)) {
            throw new UnauthorizedOperationException("You cannot delete your own account");
        }

        if (!adminRepository.findById(deletedBy)
                .map(admin -> admin.getRole() == Role.SUPER_ADMIN)
                .orElse(false)) {
            throw new UnauthorizedOperationException("Only SUPER_ADMIN can delete admins");
        }
    }
}
