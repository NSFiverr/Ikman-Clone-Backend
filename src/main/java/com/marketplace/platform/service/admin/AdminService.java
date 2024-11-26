package com.marketplace.platform.service.admin;

import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AdminService {
    /**
     * Create a new admin user
     * @param adminCreationRequest Details for admin creation
     * @return AdminResponse with created admin details
     */

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    AdminResponse createAdmin(AdminCreationRequest adminCreationRequest);

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    Page<AdminResponse> getAdmin(AdminSearchCriteria criteria, Pageable pageable);

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    AdminResponse getAdminById(Long adminId);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    AdminResponse updateAdmin(Long adminId, UpdateAdminRequest updateAdminRequest);

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    void deleteAdmin(Long adminId, AdminDeletionRequest request, Long deletedBy);

}