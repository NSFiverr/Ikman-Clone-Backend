package com.marketplace.platform.service.admin;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.AdminType;
import com.marketplace.platform.domain.audit.AdminAuditLog;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.exception.UnauthorizedOperationException;
import com.marketplace.platform.mapper.AdminMapper;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.audit.AdminAuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepository auditLogRepository;



    @Override
    @Transactional(readOnly = true)
    public Page<AdminResponse> getAdmin(AdminSearchCriteria criteria, Pageable pageable) {
        Specification<Admin> spec = buildSpecification(criteria);
        return adminRepository.findAll(spec, pageable)
                .map(adminMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponse getAdminById(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        return adminMapper.toResponse(admin);
    }

    private Specification<Admin> buildSpecification(AdminSearchCriteria criteria) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<>();

            if (criteria != null) {
                if (StringUtils.hasText(criteria.getEmail())) {
                    predicates.add(cb.like(cb.lower(root.get("email")),
                            "%" + criteria.getEmail().toLowerCase() + "%"));
                }

                if (criteria.getAdminType() != null) {
                    predicates.add(cb.equal(root.get("adminType"), criteria.getAdminType()));
                }

                if (criteria.getCreatedAfter() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"), criteria.getCreatedAfter()));
                }

                if (criteria.getCreatedBefore() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"), criteria.getCreatedBefore()));
                }

                if (StringUtils.hasText(criteria.getPermissions())) {
                    predicates.add(cb.like(root.get("permissions"),
                            "%" + criteria.getPermissions() + "%"));
                }
            }

            return predicates.isEmpty()
                    ? null
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }




    @Override
    @Transactional
    public AdminResponse createAdmin(AdminCreationRequest adminCreationRequest) {
        // Check if admin with the email already exists
        if (adminRepository.findByEmail(adminCreationRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Admin with this email already exists");
        }

        // Create admin entity using mapper
        Admin admin = adminMapper.toEntity(adminCreationRequest);

        // Save admin
        Admin savedAdmin = adminRepository.save(admin);

        // Convert to response DTO using mapper
        return adminMapper.toResponse(savedAdmin);
    }


    @Override
    @Transactional
    public AdminResponse updateAdmin(Long adminId, UpdateAdminRequest updateAdminRequest) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        // Check if new email is already taken by another admin
        if (updateAdminRequest.getEmail() != null &&
                !updateAdminRequest.getEmail().equals(admin.getEmail()) &&
                adminRepository.existsByEmail(updateAdminRequest.getEmail())) {
            throw new RuntimeException("Email is already taken");
        }

        // Update fields if they are present in the request
        if (updateAdminRequest.getEmail() != null) {
            admin.setEmail(updateAdminRequest.getEmail());
        }
        if (updateAdminRequest.getPassword() != null) {
            admin.setPasswordHash(passwordEncoder.encode(updateAdminRequest.getPassword()));
        }
        if (updateAdminRequest.getAdminType() != null) {
            admin.setAdminType(updateAdminRequest.getAdminType());
        }
        if (updateAdminRequest.getPermissions() != null) {
            admin.setPermissions(updateAdminRequest.getPermissions());
        }

        Admin updatedAdmin = adminRepository.save(admin);
        return adminMapper.toResponse(updatedAdmin);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long adminId, AdminDeletionRequest request, Long deletedBy) {
        // Get the admin who is performing the deletion
        Admin performer = adminRepository.findById(deletedBy)
                .orElseThrow(() -> new ResourceNotFoundException("Performing admin not found"));

        // Check if the performer is a SUPER_ADMIN
        if (performer.getAdminType() != AdminType.SUPER_ADMIN) {
            throw new UnauthorizedOperationException("Only super admins can delete other admins");
        }

        // Get the admin to be deleted
        Admin adminToDelete = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin to delete not found"));

        // Prevent deletion of own account
        if (adminToDelete.getAdminId().equals(deletedBy)) {
            throw new UnauthorizedOperationException("Admins cannot delete their own accounts");
        }

        // Create audit log
        AdminAuditLog auditLog = adminMapper.toAuditLog(
                adminToDelete,
                "ADMIN_DELETED",
                deletedBy,
                request.getReason()
        );

        // Soft delete the admin
        adminToDelete.setDeleted(true);
        adminToDelete.setDeletedAt(LocalDateTime.now());
        adminToDelete.setDeletedBy(deletedBy);

        // Save both the audit log and the updated admin
        auditLogRepository.save(auditLog);
        adminRepository.save(adminToDelete);
    }
}