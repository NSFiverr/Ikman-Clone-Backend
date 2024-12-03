package com.marketplace.platform.service.admin;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.audit.AdminAuditLog;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import com.marketplace.platform.dto.response.DashboardStatsResponse;
import com.marketplace.platform.exception.EmailException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.exception.UnauthorizedOperationException;
import com.marketplace.platform.mapper.AdminMapper;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import com.marketplace.platform.repository.advertisement.AdvertisementRepository;
import com.marketplace.platform.repository.audit.AdminAuditLogRepository;
import com.marketplace.platform.repository.category.CategoryRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.service.auth.JwtService;
import com.marketplace.platform.service.email.EmailService;
import com.marketplace.platform.validator.admin.AdminValidator;
import jakarta.mail.MessagingException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepository auditLogRepository;

    private final AdminValidator adminValidator;
    private final EmailService emailService;

    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final CategoryRepository categoryRepository;
    private final AdPackageRepository packageRepository;

    private final JwtService jwtService;


    @Override
    @Transactional(readOnly = true)
    public Page<AdminResponse> getAdmins(AdminSearchCriteria criteria, Pageable pageable) {
        Specification<Admin> spec = buildSpecification(criteria);
        return adminRepository.findActive(pageable)
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

                if (criteria.getRole() != null) {
                    predicates.add(cb.equal(root.get("role"), criteria.getRole()));
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
    public AdminResponse createAdmin(AdminCreationRequest request) {
        adminValidator.validateAdminCreation(request);
        try {
            String temporaryPassword = PasswordGenerator.generateSecurePassword();
            String passwordHash = passwordEncoder.encode(temporaryPassword);

            Admin admin = adminMapper.toEntity(request, passwordHash);
            Admin savedAdmin = adminRepository.save(admin);

            // Send credentials email
            emailService.sendAdminCredentials(
                    request.getEmail(),
                    request.getFirstName(),
                    temporaryPassword
            );

            return adminMapper.toResponse(savedAdmin);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send admin credentials email", e);
        }

    }


    @Override
    @Transactional
    public AdminResponse updateAdmin(Long adminId, UpdateAdminRequest updateAdminRequest) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        adminValidator.validateAdminUpdate(adminId, updateAdminRequest);

        // Update fields if they are present in the request
        if (updateAdminRequest.getEmail() != null) {
            admin.setEmail(updateAdminRequest.getEmail());
        }
        if (updateAdminRequest.getFirstName() != null) {
            admin.setFirstName(updateAdminRequest.getFirstName());
        }
        if (updateAdminRequest.getLastName() != null) {
            admin.setLastName(updateAdminRequest.getLastName());
        }
        if (updateAdminRequest.getPassword() != null) {
            admin.setPasswordHash(passwordEncoder.encode(updateAdminRequest.getPassword()));
            admin.setIsPasswordChanged(true);

            try {
                emailService.sendAdminPasswordChangeNotification(
                        admin.getEmail(),
                        admin.getFirstName()
                );
            } catch (MessagingException e) {
                // Log but don't fail the update
                log.warn("Failed to send password change notification to admin: {}", admin.getEmail(), e);
            }
        }
        if (updateAdminRequest.getPermissions() != null) {
            admin.setPermissions(updateAdminRequest.getPermissions());
        }

        Admin updatedAdmin = adminRepository.save(admin);
        return adminMapper.toResponse(updatedAdmin);
    }
    @Override
    @Transactional
    public void deleteAdmin(Long adminId, AdminDeletionRequest request, String token) {
        Optional<Admin> optionalCurrentAdmin = jwtService.getAdminFromToken(token);

        Admin currentAdmin = optionalCurrentAdmin.orElseThrow(() -> {
            log.error("Admin not found for the given access token");
            return new ResourceNotFoundException("Super admin performing the action is not found");
        });
        adminValidator.validateAdminDeletion(adminId, request, currentAdmin.getAdminId());

        Admin adminToDelete = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin to delete not found"));

        // Create audit log
        AdminAuditLog auditLog = adminMapper.toAuditLog(
                adminToDelete,
                "ADMIN_DELETED",
                currentAdmin.getAdminId(),
                request.getReason()
        );

        // Soft delete the admin
        adminToDelete.setDeleted(true);
        adminToDelete.setDeletedAt(LocalDateTime.now());
        adminToDelete.setDeletedBy(currentAdmin.getAdminId());

        // Save both the audit log and the updated admin
        auditLogRepository.save(auditLog);
        adminRepository.save(adminToDelete);
    }


    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalAdmins(adminRepository.countActive())
                .totalPackages(packageRepository.count())
                .totalCategories(categoryRepository.count())
                .totalAdvertisements(advertisementRepository.count())
                .activeAdvertisements(advertisementRepository.countByStatus(AdStatus.ACTIVE))
                .todayStats(DashboardStatsResponse.DailyStats.builder()
                        .newUsers(userRepository.countByCreatedAtAfter(startOfDay))
                        .newAdvertisements(advertisementRepository.countByCreatedAtAfter(startOfDay))
                        .totalViews(advertisementRepository.getTotalViewsForDate(startOfDay))
                        .build())
                .build();
    }
}