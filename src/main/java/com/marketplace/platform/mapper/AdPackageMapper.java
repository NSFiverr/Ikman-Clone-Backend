package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.advertisement.AdPackage;
import com.marketplace.platform.dto.request.AdPackageCreateRequest;
import com.marketplace.platform.dto.response.AdPackageResponse;
import org.springframework.stereotype.Component;

@Component
public class AdPackageMapper {

    public AdPackage toEntity(AdPackageCreateRequest request) {
        AdPackage adPackage = new AdPackage();
        adPackage.setName(request.getName());
        adPackage.setPrice(request.getPrice());
        adPackage.setDurationDays(request.getDurationDays());
        adPackage.setMaxMediaItems(request.getMaxMediaItems());
        adPackage.setMaxDocuments(request.getMaxDocuments());
        adPackage.setHasRenewalOption(request.getHasRenewalOption());
        adPackage.setHasFeaturedListing(request.getHasFeaturedListing());
        adPackage.setHasTopAd(request.getHasTopAd());
        adPackage.setHasPrioritySupport(request.getHasPrioritySupport());
        adPackage.setVisibilityLevel(request.getVisibilityLevel());
        adPackage.setFeaturedDurationDays(request.getFeaturedDurationDays());
        return adPackage;
    }

    public AdPackageResponse toResponse(AdPackage adPackage) {
        return AdPackageResponse.builder()
                .packageId(adPackage.getPackageId())
                .name(adPackage.getName())
                .price(adPackage.getPrice())
                .durationDays(adPackage.getDurationDays())
                .maxMediaItems(adPackage.getMaxMediaItems())
                .maxDocuments(adPackage.getMaxDocuments())
                .hasRenewalOption(adPackage.getHasRenewalOption())
                .hasFeaturedListing(adPackage.getHasFeaturedListing())
                .hasTopAd(adPackage.getHasTopAd())
                .hasPrioritySupport(adPackage.getHasPrioritySupport())
                .visibilityLevel(adPackage.getVisibilityLevel())
                .featuredDurationDays(adPackage.getFeaturedDurationDays())
                .createdAt(adPackage.getCreatedAt())
                .build();
    }

    public void updateEntity(AdPackage adPackage, AdPackageCreateRequest request) {
        adPackage.setName(request.getName());
        adPackage.setPrice(request.getPrice());
        adPackage.setDurationDays(request.getDurationDays());
        adPackage.setMaxMediaItems(request.getMaxMediaItems());
        adPackage.setMaxDocuments(request.getMaxDocuments());
        adPackage.setHasRenewalOption(request.getHasRenewalOption());
        adPackage.setHasFeaturedListing(request.getHasFeaturedListing());
        adPackage.setHasTopAd(request.getHasTopAd());
        adPackage.setHasPrioritySupport(request.getHasPrioritySupport());
        adPackage.setVisibilityLevel(request.getVisibilityLevel());
        adPackage.setFeaturedDurationDays(request.getFeaturedDurationDays());
    }
}
