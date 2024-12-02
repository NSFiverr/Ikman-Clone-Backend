package com.marketplace.platform.service.admin;

import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import com.marketplace.platform.dto.response.DashboardStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AdminService {
    AdminResponse createAdmin(AdminCreationRequest adminCreationRequest);

    Page<AdminResponse> getAdmin(AdminSearchCriteria criteria, Pageable pageable);

    AdminResponse getAdminById(Long adminId);

    AdminResponse updateAdmin(Long adminId, UpdateAdminRequest updateAdminRequest);

    void deleteAdmin(Long adminId, AdminDeletionRequest request, Long deletedBy);

    DashboardStatsResponse getDashboardStats();


}