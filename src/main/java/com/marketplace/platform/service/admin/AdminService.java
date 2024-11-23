package com.marketplace.platform.service.admin;

import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    /**
     * Create a new admin user
     * @param adminCreationRequest Details for admin creation
     * @return AdminResponse with created admin details
     */
    AdminResponse createAdmin(AdminCreationRequest adminCreationRequest);

    Page<AdminResponse> getAdmin(AdminSearchCriteria criteria, Pageable pageable);
    AdminResponse getAdminById(Long adminId);


    AdminResponse updateAdmin(Long adminId, UpdateAdminRequest updateAdminRequest);
}