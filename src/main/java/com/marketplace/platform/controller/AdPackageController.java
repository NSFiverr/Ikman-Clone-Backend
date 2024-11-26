//package com.marketplace.platform.controller;
//
//import com.marketplace.platform.dto.request.AdPackageCreateRequest;
//import com.marketplace.platform.dto.response.AdPackageResponse;
//import com.marketplace.platform.service.advertisement.AdPackageService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/packages")
//@RequiredArgsConstructor
//public class AdPackageController {
//    private final AdPackageService adPackageService;
//
//    @PostMapping
//    public ResponseEntity<AdPackageResponse> createPackage(@Valid @RequestBody AdPackageCreateRequest request) {
//        return new ResponseEntity<>(adPackageService.createPackage(request), HttpStatus.CREATED);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<AdPackageResponse> updatePackage(
//            @PathVariable Long id,
//            @Valid @RequestBody AdPackageCreateRequest request) {
//        return ResponseEntity.ok(adPackageService.updatePackage(id, request));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<AdPackageResponse> getPackage(@PathVariable Long id) {
//        return ResponseEntity.ok(adPackageService.getPackage(id));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<AdPackageResponse>> getAllActivePackages() {
//        return ResponseEntity.ok(adPackageService.getAllActivePackages());
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
//        adPackageService.deletePackage(id);
//        return ResponseEntity.noContent().build();
//    }
//}
package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.AdPackageCreateRequest;
import com.marketplace.platform.dto.response.AdPackageResponse;
import com.marketplace.platform.service.advertisement.AdPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class AdPackageController {
    private final AdPackageService adPackageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdPackageResponse> createPackage(@Valid @RequestBody AdPackageCreateRequest request) {
        return new ResponseEntity<>(adPackageService.createPackage(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdPackageResponse> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody AdPackageCreateRequest request) {
        return ResponseEntity.ok(adPackageService.updatePackage(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdPackageResponse> getPackage(@PathVariable Long id) {
        return ResponseEntity.ok(adPackageService.getPackage(id));
    }

    @GetMapping
    public ResponseEntity<List<AdPackageResponse>> getAllActivePackages() {
        return ResponseEntity.ok(adPackageService.getAllActivePackages());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        adPackageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}