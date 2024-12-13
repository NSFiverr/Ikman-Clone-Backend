package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.advertisement.*;
import com.marketplace.platform.domain.category.CategoryVersion;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdvertisementMapper {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public AdvertisementResponse toResponse(Advertisement advertisement, Boolean isFavorited, Boolean isOwner) {
        return AdvertisementResponse.builder()
                .adId(advertisement.getAdId())
                .title(advertisement.getTitle())
                .description(advertisement.getDescription())
                .price(advertisement.getPrice())
                .isNegotiable(advertisement.getIsNegotiable())
                .itemCondition(advertisement.getItemCondition())
                .status(advertisement.getStatus())
                .isFeatured(advertisement.getIsFeatured())
                .isTopAd(advertisement.getIsTopAd())
                .viewCount(advertisement.getViewCount())
                .latitude(advertisement.getLocationCoordinates().getY())
                .longitude(advertisement.getLocationCoordinates().getX())
                .address(advertisement.getAddress())
                .createdAt(advertisement.getCreatedAt())
                .updatedAt(advertisement.getUpdatedAt())
                .expiresAt(advertisement.getExpiresAt())
                .featuredUntil(advertisement.getFeaturedUntil())
                .topAdUntil(advertisement.getTopAdUntil())
                .user(userMapper.toResponse(advertisement.getUser()))
                .category(categoryMapper.toResponse(
                        advertisement.getCategoryVersion().getCategory(),
                        advertisement.getCategoryVersion()))
                .adPackage(toPackageResponse(advertisement.getAdPackage()))
                .attributes(toAttributeResponses(advertisement.getAttributes()))
                .mediaItems(toMediaResponses(advertisement.getMediaItems()))
                .paymentProof(toPaymentProofResponse(advertisement.getPaymentProof()))
                .favoriteCount((long) advertisement.getFavorites().size())
                .isFavorited(isFavorited)
                .isOwner(isOwner)
                .build();
    }

    private PackageResponse toPackageResponse(AdPackage adPackage) {
        return PackageResponse.builder()
                .packageId(adPackage.getPackageId())
                .name(adPackage.getName())
                .price(adPackage.getPrice())
                .durationDays(adPackage.getDurationDays())
                .maxMediaItems(adPackage.getMaxMediaItems())
                .hasFeaturedListing(adPackage.getHasFeaturedListing())
                .hasTopAd(adPackage.getHasTopAd())
                .visibilityLevel(adPackage.getVisibilityLevel())
                .build();
    }

    private Set<AdAttributeResponse> toAttributeResponses(Set<AdAttribute> attributes) {
        return attributes.stream()
                .map(this::toAttributeResponse)
                .collect(Collectors.toSet());
    }

    private AdAttributeResponse toAttributeResponse(AdAttribute attribute) {
        AdAttributeResponse.AdAttributeResponseBuilder builder = AdAttributeResponse.builder()
                .attributeDefinitionId(attribute.getAttributeDefinition().getAttrDefId())
                .name(attribute.getAttributeDefinition().getName())
                .displayName(attribute.getAttributeDefinition().getDisplayName())
                .textValue(attribute.getTextValue())
                .numericValue(attribute.getNumericValue())
                .dateValue(attribute.getDateValue());

        if (attribute.getLocationCoordinates() != null) {
            builder.latitude(attribute.getLocationCoordinates().getY())
                    .longitude(attribute.getLocationCoordinates().getX());
        }

        return builder.build();
    }

    private Set<MediaResponse> toMediaResponses(Set<AdMedia> mediaItems) {
        return mediaItems.stream()
                .map(this::toMediaResponse)
                .collect(Collectors.toSet());
    }

    private MediaResponse toMediaResponse(AdMedia media) {
        return MediaResponse.builder()
                .mediaId(media.getMediaId())
                .mediaType(media.getMediaType().name())
                .firebaseUrl(media.getFirebaseUrl())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .displayOrder(media.getDisplayOrder())
                .build();
    }

    private PaymentProofResponse toPaymentProofResponse(PaymentProof paymentProof) {
        if (paymentProof == null) {
            return null;
        }

        return PaymentProofResponse.builder()
                .id(paymentProof.getId())
                .firebaseUrl(paymentProof.getFirebaseUrl())
                .originalFilename(paymentProof.getOriginalFilename())
                .verifiedBy(paymentProof.getVerifiedBy())
                .verifiedAt(paymentProof.getVerifiedAt())
                .createdAt(paymentProof.getCreatedAt())
                .build();
    }

    public AdvertisementCreateRequest toCreateRequest(AdvertisementUpdateRequest updateRequest, Advertisement existingAd) {
        AdvertisementCreateRequest createRequest = new AdvertisementCreateRequest();
        createRequest.setCategoryId(existingAd.getCategoryVersion().getCategory().getCategoryId());
        createRequest.setPackageId(existingAd.getAdPackage().getPackageId());
        createRequest.setTitle(updateRequest.getTitle());
        createRequest.setDescription(updateRequest.getDescription());
        createRequest.setPrice(updateRequest.getPrice());
        createRequest.setLatitude(updateRequest.getLatitude());
        createRequest.setLongitude(updateRequest.getLongitude());
        createRequest.setAddress(updateRequest.getAddress());
        createRequest.setIsNegotiable(updateRequest.getIsNegotiable());
        createRequest.setItemCondition(updateRequest.getItemCondition());
        createRequest.setAttributes(updateRequest.getAttributes());
        createRequest.setMediaItems(updateRequest.getMediaItems());

        return createRequest;
    }
}