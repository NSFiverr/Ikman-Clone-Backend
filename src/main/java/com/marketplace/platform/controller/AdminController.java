package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;
import com.marketplace.platform.dto.response.DashboardStatsResponse;
import com.marketplace.platform.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<Page<AdminResponse>> getAdmins(
            @ModelAttribute AdminSearchCriteria criteria,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAdmins(criteria, pageable));
    }

    @GetMapping("/{adminId}")
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdminById(adminId));
    }

    @PostMapping
    public ResponseEntity<AdminResponse> createAdmin(
            @Valid @RequestBody AdminCreationRequest adminCreationRequest) {
        AdminResponse createdAdmin = adminService.createAdmin(adminCreationRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdAdmin);
    }

    @PutMapping("/{adminId}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminRequest updateAdminRequest) {
        AdminResponse updatedAdmin = adminService.updateAdmin(adminId, updateAdminRequest);
        return ResponseEntity.ok(updatedAdmin);
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long adminId,
            @RequestParam String reason,
            @RequestHeader("Authorization") String accessToken) {

        AdminDeletionRequest request = new AdminDeletionRequest();
        request.setReason(reason);

        adminService.deleteAdmin(adminId, request, accessToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }
}