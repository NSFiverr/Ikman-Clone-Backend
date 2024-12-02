package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.*;
import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.domain.category.CategoryVersion;
import com.marketplace.platform.domain.category.CategoryVersionAttribute;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.projection.AdvertisementListingProjection;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.mapper.AdvertisementMapper;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import com.marketplace.platform.repository.advertisement.AdvertisementRepository;
import com.marketplace.platform.repository.advertisement.AdvertisementSearchSpecification;
import com.marketplace.platform.service.auth.JwtService;
import com.marketplace.platform.service.category.CategoryVersioningService;
import com.marketplace.platform.service.storage.FirebaseStorageService;
import com.marketplace.platform.validator.advertisement.AdvertisementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private final AdvertisementSearchSpecification searchSpecification;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    private final JwtService jwtService;


    @Override
    @Transactional
    public AdvertisementResponse createAdvertisement(AdvertisementCreateRequest request, String  token) {
        Optional<User> optionalCurrentUser = jwtService.getUserFromToken(token);
        validator.validateCreateRequest(request);
        Set<String> uploadedPaths = new HashSet<>();

        try {
            CategoryVersion categoryVersion = categoryVersioningService.getCategoryCurrentVersion(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Invalid category"));

            AdPackage adPackage = adPackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new BadRequestException("Invalid package"));

            // Get the initial status based on content validation
            AdStatus initialStatus = validator.validateContentAndGetStatus(request);

            Advertisement advertisement = optionalCurrentUser.map(currentUser ->
                            buildBaseAdvertisement(request, initialStatus, currentUser, categoryVersion, adPackage))
                    .orElseThrow(() -> {
                        log.error("User not found for the given access token");
                        return new ResourceNotFoundException("User not found");
                    });


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
    @Transactional
    public AdvertisementResponse approveAdvertisement(Long adId) {
        Advertisement advertisement = advertisementRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found"));

        if (advertisement.getStatus() != AdStatus.PENDING_REVIEW) {
            throw new BadRequestException("Advertisement is not pending review");
        }

        advertisement.setStatus(AdStatus.ACTIVE);
        advertisement.setUpdatedAt(LocalDateTime.now());
        advertisement = advertisementRepository.save(advertisement);

        return advertisementMapper.toResponse(advertisement, false, false);
    }

    @Override
    @Transactional
    public AdvertisementResponse rejectAdvertisement(Long adId) {
        Advertisement advertisement = advertisementRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found"));

        if (advertisement.getStatus() != AdStatus.PENDING_REVIEW) {
            throw new BadRequestException("Advertisement is not pending review");
        }

        advertisement.setStatus(AdStatus.SUSPENDED);
        advertisement.setUpdatedAt(LocalDateTime.now());
        advertisement = advertisementRepository.save(advertisement);

        return advertisementMapper.toResponse(advertisement, false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdvertisementResponse> getPendingReviewAdvertisements(Pageable pageable) {
        return advertisementRepository.findByStatus(AdStatus.PENDING_REVIEW, pageable)
                .map(ad -> advertisementMapper.toResponse(ad, false, false));
    }

    @Override
    @Transactional(readOnly = true)
    public AdvertisementResponse getAdvertisement(Long id, String token) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        User currentUser = jwtService.getUserFromToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User Owner = advertisement.getUser();

        Boolean isOwner = currentUser == Owner;

        // Increment view count
        if(!isOwner){
            advertisement.setViewCount(advertisement.getViewCount() + 1);
            advertisementRepository.save(advertisement);
        }


        return advertisementMapper.toResponse(advertisement, false, isOwner);
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
        Set<AdStatus> statuses = EnumSet.of(
                AdStatus.ACTIVE,
                AdStatus.PENDING_REVIEW,
                AdStatus.SUSPENDED
        );

        return advertisementRepository
                .findByUserUserIdAndStatusIn(userId, statuses, pageable)
                .map(ad -> advertisementMapper.toResponse(ad, false, true));
    }

    @Override
    @Transactional
    public AdvertisementResponse updateAdvertisement(
            Long id,
            AdvertisementUpdateRequest request,
            List<MultipartFile> newFiles,
            List<MediaType> newMediaTypes,
            List<Integer> newDisplayOrders,
            List<Long> retainedMediaIds,
            String token
    ) {
        Advertisement existingAd = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        User currentUser = jwtService.getUserFromToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validator.validateUpdateRequest(request, existingAd, currentUser);

        Set<String> uploadedPaths = new HashSet<>();
        try {
            AdvertisementCreateRequest refinedReq = advertisementMapper.toCreateRequest(request, existingAd);

            // Update basic fields
            updateBasicFields(existingAd, refinedReq);

            // Handle media updates
            if (retainedMediaIds != null || newFiles != null) {
                updateMediaItems(
                        existingAd,
                        retainedMediaIds,
                        newFiles,
                        newMediaTypes,
                        newDisplayOrders,
                        uploadedPaths
                );
            }

            // Update attributes
            existingAd.getAttributes().clear();
            processAttributes(existingAd, refinedReq, existingAd.getCategoryVersion());

            existingAd = advertisementRepository.save(existingAd);
            return advertisementMapper.toResponse(existingAd, false, true);

        } catch (Exception e) {
            // Rollback any new uploads
            rollbackUploads(uploadedPaths);
            throw e instanceof BadRequestException ? (BadRequestException) e :
                    new BadRequestException("Failed to update advertisement: " + e.getMessage());
        }
    }

    private void updateBasicFields(Advertisement existingAd, AdvertisementCreateRequest refinedReq) {
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
    }

    private void updateMediaItems(
            Advertisement existingAd,
            List<Long> retainedMediaIds,
            List<MultipartFile> newFiles,
            List<MediaType> newMediaTypes,
            List<Integer> newDisplayOrders,
            Set<String> uploadedPaths
    ) throws IOException {
        // Get media to keep if any IDs are specified
        final Set<AdMedia> retainedMedia = (retainedMediaIds != null && !retainedMediaIds.isEmpty())
                ? existingAd.getMediaItems().stream()
                .filter(media -> retainedMediaIds.contains(media.getMediaId()))
                .collect(Collectors.toSet())
                : new HashSet<>();

        final Set<AdMedia> mediaToDelete = existingAd.getMediaItems().stream()
                .filter(media -> !retainedMedia.contains(media))
                .collect(Collectors.toSet());

        // Delete non-retained media files
        for (AdMedia media : mediaToDelete) {
            try {
                firebaseStorageService.deleteFile(media.getFirebaseUrl());
                adMediaService.deleteAdMediaFromDB(media);
            } catch (Exception e) {
                log.error("Failed to delete media file: {}", media.getFirebaseUrl(), e);
                throw new BadRequestException("Failed to delete media file");
            }
        }

        // Handle new uploads if any
        Set<AdMedia> newMediaItems = new HashSet<>();
        if (newFiles != null && !newFiles.isEmpty()) {
            Set<MediaRequest> mediaRequests = adMediaService.createMediaRequests(
                    newFiles,
                    newMediaTypes,
                    newDisplayOrders
            );
            newMediaItems = adMediaService.uploadMediaBatch(existingAd, mediaRequests, uploadedPaths);
        }

        // Clear and update the advertisement's media items
        existingAd.getMediaItems().clear();
        existingAd.getMediaItems().addAll(retainedMedia);
        existingAd.getMediaItems().addAll(newMediaItems);
    }

    private void rollbackUploads(Set<String> uploadedPaths) {
        uploadedPaths.forEach(path -> {
            try {
                firebaseStorageService.deleteFile(path);
            } catch (Exception ignored) {
                log.error("Failed to delete file during rollback: {}", path);
            }
        });
    }
    @Override
    @Transactional
    public void deleteAdvertisement(Long id, String token) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertisement not found with id: " + id));

        Optional<User> optionalCurrentUser = jwtService.getUserFromToken(token);

        User currentUser = optionalCurrentUser.orElseThrow(() -> {
            log.error("User not found for the given access token");
            return new ResourceNotFoundException("User not found");
        });

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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "advertisements",
            key = "'search:' + #criteria.toString() + ':page:' + #pageable.pageNumber",
            unless = "#result.totalElements == 0")
    public Page<AdvertisementListingProjection> searchAdvertisements(
            AdvertisementSearchCriteria criteria,
            Pageable pageable) {

        criteria.validateAndClean(); // Clean and validate input

        if (criteria.hasLocationCriteria()) {
            return advertisementRepository.findNearbyAdvertisements(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getRadius() * 1000, // convert km to meters
                    criteria.getSearchTerm(),
                    criteria.getCategoryId(),
                    criteria.getCategoryIds(),
                    criteria.getMinPrice(),
                    criteria.getMaxPrice(),
                    criteria.getCondition() != null ? criteria.getCondition().name() : null,
                    criteria.getVerifiedSellersOnly(),
                    criteria.getNegotiable(),
                    criteria.getHasPhotos(),
                    criteria.getFeatured(),
                    criteria.getTopAdsOnly(),
                    criteria.getSellerId(),
                    criteria.getMinViewCount(),
                    criteria.getPostedAfter(),
                    criteria.getPostedBefore(),
                    criteria.getExcludeAdIds(),
                    criteria.getSortBy(),
                    criteria.getSortDirection(),
                    pageable
            );
        }

        // Use optimized projection query for non-location-based search
        return advertisementRepository.findAdvertisementsForListing(
                criteria.getSearchTerm(),
                criteria.getCategoryId(),
                criteria.getCategoryIds(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getCondition() != null ? criteria.getCondition().name() : null,
                criteria.getVerifiedSellersOnly(),
                criteria.getNegotiable(),
                criteria.getHasPhotos(),
                criteria.getFeatured(),
                criteria.getTopAdsOnly(),
                criteria.getSellerId(),
                criteria.getMinViewCount(),
                criteria.getPostedAfter(),
                criteria.getPostedBefore(),
                criteria.getExcludeAdIds(),
                criteria.getSortBy(),
                criteria.getSortDirection(),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "advertisements",
            key = "'nearby:' + #criteria.toString() + ':page:' + #pageable.pageNumber",
            unless = "#result.totalElements == 0")
    public Page<AdvertisementListingProjection> getNearbyAdvertisements(
            AdvertisementSearchCriteria criteria,
            Pageable pageable) {

        criteria.validateAndClean(); // Use the existing validation method

        return advertisementRepository.findNearbyAdvertisements(
                criteria.getLatitude(),
                criteria.getLongitude(),
                criteria.getRadius() * 1000, // Convert to meters
                criteria.getSearchTerm(),
                criteria.getCategoryId(),
                criteria.getCategoryIds(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getCondition() != null ? criteria.getCondition().name() : null,
                criteria.getVerifiedSellersOnly(),
                criteria.getNegotiable(),
                criteria.getHasPhotos(),
                criteria.getFeatured(),
                criteria.getTopAdsOnly(),
                criteria.getSellerId(),
                criteria.getMinViewCount(),
                criteria.getPostedAfter(),
                criteria.getPostedBefore(),
                criteria.getExcludeAdIds(),
                criteria.getSortBy(),
                criteria.getSortDirection(),
                pageable
        );
    }


    //helper functions
    private Advertisement buildBaseAdvertisement(
            AdvertisementCreateRequest request,
            AdStatus initialStatus,
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
                .status(initialStatus)
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