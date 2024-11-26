package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.*;
import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.domain.category.CategoryVersion;
import com.marketplace.platform.domain.category.CategoryVersionAttribute;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.AdAttributeRequest;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.mapper.AdvertisementMapper;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import com.marketplace.platform.repository.advertisement.AdvertisementRepository;
import com.marketplace.platform.service.category.CategoryVersioningService;
import com.marketplace.platform.service.storage.FirebaseStorageService;
import com.marketplace.platform.validator.advertisement.AdvertisementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementServiceImpl implements AdvertisementService {
    private final AdvertisementRepository advertisementRepository;
    private final AdPackageRepository adPackageRepository;
    private final CategoryVersioningService categoryVersioningService;

    private final AdMediaService adMediaService;
    private final FirebaseStorageService firebaseStorageService;
    private final AdvertisementMapper advertisementMapper;
    private final AdvertisementValidator validator;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    @Transactional
    public AdvertisementResponse createAdvertisement(AdvertisementCreateRequest request, User currentUser) {
        validator.validateCreateRequest(request);
        Set<String> uploadedPaths = new HashSet<>();

        try {
            User testUser = new User();
            testUser.setUserId(1L);
            testUser.setEmail("test@example.com");
            testUser.setFirstName("John");
            testUser.setLastName("Doe");
            testUser.setPhone("1234567890");

            CategoryVersion categoryVersion = categoryVersioningService.getCategoryCurrentVersion(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Invalid category"));

            AdPackage adPackage = adPackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new BadRequestException("Invalid package"));

            Advertisement advertisement = buildBaseAdvertisement(request, testUser, categoryVersion, adPackage);

            Set<AdMedia> mediaItems = new HashSet<>();
            if (request.getMediaItems() != null && !request.getMediaItems().isEmpty()) {
                mediaItems = adMediaService.uploadMediaBatch(advertisement, request.getMediaItems(), uploadedPaths);
                advertisement.setMediaItems(mediaItems);
            }

            processAttributes(advertisement, request, categoryVersion);
            advertisement = advertisementRepository.save(advertisement);

            return advertisementMapper.toResponse(advertisement, false, true);
        } catch (Exception e) {
            // Rollback any uploaded files on any exception
            uploadedPaths.forEach(path -> {
                try {
                    firebaseStorageService.deleteFile(path);
                } catch (Exception ignored) {
                    log.error("Failed to delete file during rollback: {}", path);
                }
            });
            throw e instanceof BadRequestException ? (BadRequestException) e : new BadRequestException("Failed to create advertisement");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdvertisementResponse getAdvertisement(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        // Increment view count
        advertisement.setViewCount(advertisement.getViewCount() + 1);
        advertisementRepository.save(advertisement);

        return advertisementMapper.toResponse(advertisement, false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdvertisementResponse> getAllAdvertisements(Pageable pageable) {
        return advertisementRepository.findByStatus(AdStatus.ACTIVE, pageable)
                .map(ad -> advertisementMapper.toResponse(ad, false, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdvertisementResponse> getUserAdvertisements(Long userId, Pageable pageable) {
        return advertisementRepository.findByUserUserIdAndStatus(userId, AdStatus.ACTIVE, pageable)
                .map(ad -> advertisementMapper.toResponse(ad, false, true));
    }

    @Override
    @Transactional
    public AdvertisementResponse updateAdvertisement(Long id, AdvertisementUpdateRequest request, User currentUser) {
        Advertisement existingAd = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        //validate update request
        validator.validateUpdateRequest(request, existingAd, currentUser);

        Set<String> uploadedPaths = new HashSet<>();
        try {
            AdvertisementCreateRequest refinedReq = advertisementMapper.toCreateRequest(request, existingAd);
            // Update basic fields
            existingAd.setTitle(refinedReq.getTitle());
            existingAd.setDescription(refinedReq.getDescription());
            existingAd.setPrice(refinedReq.getPrice());
            existingAd.setLocationCoordinates(
                    geometryFactory.createPoint(
                            new Coordinate(refinedReq.getLongitude(), refinedReq.getLatitude())
                    )
            );
            existingAd.setAddress(refinedReq.getAddress());
            existingAd.setIsNegotiable(refinedReq.getIsNegotiable());
            existingAd.setItemCondition(refinedReq.getItemCondition());
            existingAd.setUpdatedAt(LocalDateTime.now());

            // Handle media updates if any
            if (request.getMediaItems() != null && !refinedReq.getMediaItems().isEmpty()) {
                // Delete existing media files from storage
                existingAd.getMediaItems().forEach(media -> {
                    try {
                        firebaseStorageService.deleteFile(media.getFirebaseUrl());
                    } catch (Exception e) {
                        log.error("Failed to delete old media file: {}", media.getFirebaseUrl(), e);
                    }
                });

                // Upload new media files
                Set<AdMedia> newMediaItems = adMediaService.uploadMediaBatch(existingAd, refinedReq.getMediaItems(), uploadedPaths);
                existingAd.getMediaItems().clear();
                existingAd.getMediaItems().addAll(newMediaItems);
            }

            // Update attributes
            existingAd.getAttributes().clear();
            processAttributes(existingAd, refinedReq, existingAd.getCategoryVersion());

            // Save updates
            existingAd = advertisementRepository.save(existingAd);
            return advertisementMapper.toResponse(existingAd, false, true);

        } catch (Exception e) {
            // Rollback any new uploads if there was an error
            uploadedPaths.forEach(path -> {
                try {
                    firebaseStorageService.deleteFile(path);
                } catch (Exception ignored) {
                    log.error("Failed to delete file during rollback: {}", path);
                }
            });
            throw e instanceof BadRequestException ? (BadRequestException) e :
                    new BadRequestException("Failed to update advertisement: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAdvertisement(Long id, User currentUser) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        validator.validateDeleteRequest(advertisement, currentUser);

        try {
            // Delete media files from storage
            advertisement.getMediaItems().forEach(media -> {
                try {
                    firebaseStorageService.deleteFile(media.getFirebaseUrl());
                } catch (Exception e) {
                    log.error("Failed to delete media file: {}", media.getFirebaseUrl(), e);
                    // Continue with deletion even if file delete fails
                }
            });

            // Soft delete the advertisement
            advertisement.setStatus(AdStatus.DELETED);
            advertisement.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(advertisement);

        } catch (Exception e) {
            throw new BadRequestException("Failed to delete advertisement: " + e.getMessage());
        }
    }

    private Advertisement buildBaseAdvertisement(
            AdvertisementCreateRequest request,
            User user,
            CategoryVersion categoryVersion,
            AdPackage adPackage
    ) {
        LocalDateTime now = LocalDateTime.now();

        return Advertisement.builder()
                .user(user)
                .categoryVersion(categoryVersion)
                .adPackage(adPackage)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .locationCoordinates(
                        geometryFactory.createPoint(
                                new Coordinate(request.getLongitude(), request.getLatitude())
                        )
                )
                .address(request.getAddress())
                .isNegotiable(request.getIsNegotiable())
                .itemCondition(request.getItemCondition())
                .status(AdStatus.ACTIVE)
                .isFeatured(adPackage.getHasFeaturedListing())
                .isTopAd(adPackage.getHasTopAd())
                .viewCount(0)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusDays(adPackage.getDurationDays()))
                .featuredUntil(adPackage.getHasFeaturedListing() ?
                        now.plusDays(adPackage.getFeaturedDurationDays()) : null)
                .mediaItems(new HashSet<>())
                .attributes(new HashSet<>())
                .build();
    }


    private void processAttributes(
            Advertisement advertisement,
            AdvertisementCreateRequest request,
            CategoryVersion categoryVersion
    ) {
        if (request.getAttributes() != null) {
            Set<AdAttribute> attributes = new HashSet<>();

            for (AdAttributeRequest attrRequest : request.getAttributes()) {
                AttributeDefinition definition = categoryVersion.getAttributes().stream()
                        .filter(attr -> attr.getAttributeDefinition().getAttrDefId()
                                .equals(attrRequest.getAttributeDefinitionId()))
                        .map(CategoryVersionAttribute::getAttributeDefinition)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Invalid attribute"));

                AdAttribute attribute = createAttribute(advertisement, attrRequest, definition);
                attributes.add(attribute);
            }

            advertisement.setAttributes(attributes);
        }
    }

    private AdAttribute createAttribute(
            Advertisement advertisement,
            AdAttributeRequest request,
            AttributeDefinition definition
    ) {
        AdAttribute.AdAttributeBuilder builder = AdAttribute.builder()
                .advertisement(advertisement)
                .attributeDefinition(definition)
                .id(new AdAttributeKey(
                        advertisement.getAdId(),
                        definition.getAttrDefId()
                ));

        switch (definition.getDataType()) {
            case TEXT:
                builder.textValue(request.getTextValue());
                break;
            case NUMBER:
                builder.numericValue(request.getNumericValue());
                break;
            case DATE:
                builder.dateValue(request.getDateValue());
                break;
            case LOCATION:
                if (request.getLatitude() != null && request.getLongitude() != null) {
                    builder.locationCoordinates(
                            geometryFactory.createPoint(
                                    new Coordinate(request.getLongitude(), request.getLatitude())
                            )
                    );
                }
                break;
        }

        return builder.build();
    }
}