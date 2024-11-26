package com.marketplace.platform.controller;


import com.marketplace.platform.domain.admin.Role;
import com.marketplace.platform.exception.BadRequestException;
import org.springframework.security.access.prepost.PreAuthorize;
import com.marketplace.platform.dto.request.AdminCreationRequest;
import com.marketplace.platform.dto.request.AdminDeletionRequest;
import com.marketplace.platform.dto.request.AdminSearchCriteria;
import com.marketplace.platform.dto.request.UpdateAdminRequest;
import com.marketplace.platform.dto.response.AdminResponse;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<AdminResponse>> getAdmin(
            @ModelAttribute AdminSearchCriteria criteria,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAdmin(criteria, pageable));
    }

    @GetMapping("/{adminId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAdminById(adminId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminResponse> createAdmin(
            @Valid @RequestBody AdminCreationRequest adminCreationRequest) {
        // Validate that SUPER_ADMIN can't create another SUPER_ADMIN
       if (adminCreationRequest.getRole() == Role.SUPER_ADMIN) {
            throw new BadRequestException("Cannot create another SUPER_ADMIN");
       }

        AdminResponse createdAdmin = adminService.createAdmin(adminCreationRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdAdmin);
    }

    @PutMapping("/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminRequest updateAdminRequest) {
        AdminResponse updatedAdmin = adminService.updateAdmin(adminId, updateAdminRequest);
        return ResponseEntity.ok(updatedAdmin);
    }

    @DeleteMapping("/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long adminId,
            @Valid @RequestBody AdminDeletionRequest request,
            @RequestAttribute("currentAdminId") Long currentAdminId) {
        adminService.deleteAdmin(adminId, request, currentAdminId);
        return ResponseEntity.noContent().build();
    }
}