package com.marketplace.platform.validator.advertisement;

import com.marketplace.platform.domain.advertisement.AdPackage;
import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.VisibilityLevel;
import com.marketplace.platform.dto.request.AdPackageCreateRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdPackageValidator {
    private final AdPackageRepository adPackageRepository;

    private static final int MAX_DURATION_DAYS = 365; // 1 year
    private static final int MAX_MEDIA_ITEMS = 50;
    private static final int MAX_DOCUMENTS = 20;
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");

    public void validateCreateRequest(AdPackageCreateRequest request) {
        validateBasicFields(request);
        validatePackageName(request.getName());
        validateFeaturedSettings(request);
        validatePricing(request.getPrice());
    }

    public void validateUpdateRequest(Long id, AdPackageCreateRequest request) {
        validateBasicFields(request);
        validateFeaturedSettings(request);
        validatePricing(request.getPrice());
        validatePackageStatus(id);
    }

    public void validateDeleteRequest(Long id) {
        validatePackageStatus(id);
        validateActiveAdvertisements(id);
    }

    private void validateBasicFields(AdPackageCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Package name is required");
        }

        if (request.getName().length() > 50) {
            throw new BadRequestException("Package name cannot exceed 50 characters");
        }

        if (request.getDurationDays() <= 0 || request.getDurationDays() > MAX_DURATION_DAYS) {
            throw new BadRequestException("Duration days must be between 1 and " + MAX_DURATION_DAYS);
        }

        if (request.getMaxMediaItems() <= 0 || request.getMaxMediaItems() > MAX_MEDIA_ITEMS) {
            throw new BadRequestException("Maximum media items must be between 1 and " + MAX_MEDIA_ITEMS);
        }

        if (request.getMaxDocuments() != null &&
                (request.getMaxDocuments() < 0 || request.getMaxDocuments() > MAX_DOCUMENTS)) {
            throw new BadRequestException("Maximum documents must be between 0 and " + MAX_DOCUMENTS);
        }

        validateVisibilityLevel(request.getVisibilityLevel());
    }

    private void validatePackageName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Package name is required");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < 3) {
            throw new BadRequestException("Package name must be at least 3 characters");
        }

        if (adPackageRepository.existsByNameAndVisible(trimmedName)) {
            throw new BadRequestException("Package with this name already exists");
        }
    }

    private void validateFeaturedSettings(AdPackageCreateRequest request) {
        if (Boolean.TRUE.equals(request.getHasFeaturedListing())) {
            if (request.getFeaturedDurationDays() == null || request.getFeaturedDurationDays() <= 0) {
                throw new BadRequestException("Featured duration days must be positive for featured listings");
            }

            if (request.getFeaturedDurationDays() > request.getDurationDays()) {
                throw new BadRequestException("Featured duration cannot exceed package duration");
            }

            // If package has featured listing, it should cost something
            if (request.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                throw new BadRequestException("Featured packages cannot be free");
            }
        }

        // Top ad validation
        if (Boolean.TRUE.equals(request.getHasTopAd()) && !Boolean.TRUE.equals(request.getHasFeaturedListing())) {
            throw new BadRequestException("Top ad feature requires featured listing");
        }
    }

    private void validatePricing(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price cannot be negative");
        }

        if (price.compareTo(MAX_PRICE) > 0) {
            throw new BadRequestException("Price cannot exceed " + MAX_PRICE);
        }

        // Validate decimal places
        if (price.scale() > 2) {
            throw new BadRequestException("Price cannot have more than 2 decimal places");
        }
    }

    private void validateVisibilityLevel(VisibilityLevel visibilityLevel) {
        if (visibilityLevel == null) {
            throw new BadRequestException("Visibility level is required");
        }

        if (visibilityLevel == VisibilityLevel.HIDDEN) {
            throw new BadRequestException("Cannot create package with HIDDEN visibility");
        }
    }

    private void validatePackageStatus(Long id) {
        AdPackage adPackage = adPackageRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Package not found"));

        if (adPackage.getVisibilityLevel() == VisibilityLevel.HIDDEN) {
            throw new BadRequestException("Package is already deleted");
        }
    }

    private void validateActiveAdvertisements(Long id) {
        AdPackage adPackage = adPackageRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Package not found"));

        if (!adPackage.getAdvertisements().isEmpty()) {
            long activeAds = adPackage.getAdvertisements().stream()
                    .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                    .count();

            if (activeAds > 0) {
                throw new BadRequestException("Cannot delete package with " + activeAds + " active advertisements");
            }
        }
    }
}