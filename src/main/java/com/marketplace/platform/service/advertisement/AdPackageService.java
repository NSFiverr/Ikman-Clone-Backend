package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.dto.request.AdPackageCreateRequest;
import com.marketplace.platform.dto.response.AdPackageResponse;

import java.util.List;

public interface AdPackageService {
    AdPackageResponse createPackage(AdPackageCreateRequest request);
    AdPackageResponse updatePackage(Long id, AdPackageCreateRequest request);
    AdPackageResponse getPackage(Long id);
    List<AdPackageResponse> getAllActivePackages();
    void deletePackage(Long id);
}
