package com.marketplace.platform.validator.advertisement;

import com.marketplace.platform.domain.category.*;
import com.marketplace.platform.domain.advertisement.*;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.mapper.AdvertisementMapper;
import com.marketplace.platform.service.category.CategoryVersioningService;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdvertisementValidator {
    private final CategoryVersioningService categoryVersioningService;
    private final AdPackageRepository adPackageRepository;
    private final AdvertisementMapper advertisementMapper;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png"
    );

    // add more words as needed
    private static final Set<String> FLAGGED_WORDS = Set.of(
            "scam", "illegal", "fake", "counterfeit", "replica",
            "prescription", "stolen", "drugs", "weapon", "hack");

    public void validateCreateRequest(AdvertisementCreateRequest request) {
        validateBasicFields(request);
        validateCategory(request);
        validateLocation(request);
        validateMedia(request);
        validatePackage(request);
    }

    public void validateUpdateRequest(AdvertisementUpdateRequest updateRequest, Advertisement existingAd, User currentUser) {
        validateUpdatePermissions(existingAd);

        AdvertisementCreateRequest request=advertisementMapper.toCreateRequest(updateRequest,existingAd);
        validateOwnership(existingAd, currentUser);
        validateBasicFields(request);
        validateLocation(request);
        validateMedia(request);
        validateUpdateAttributes(request.getAttributes(), existingAd.getCategoryVersion());
    }

    public void validateDeleteRequest(Advertisement advertisement, User currentUser) {
        validateOwnership(advertisement, currentUser);
        validateNotDeleted(advertisement);
    }

    private void validateUpdatePermissions(Advertisement advertisement) {
        if (advertisement.getStatus() != AdStatus.ACTIVE) {
            throw new BadRequestException("Only active ads can be updated");
        }
    }

    private void validateOwnership(Advertisement advertisement, User currentUser) {
        if (!advertisement.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You don't have permission to perform this operation");
        }
    }

    private void validateNotDeleted(Advertisement advertisement) {
        if (advertisement.getStatus() == AdStatus.DELETED) {
            throw new BadRequestException("Advertisement is already deleted");
        }
    }

    private void validateUpdateAttributes(Set<AdAttributeRequest> attributes, CategoryVersion categoryVersion) {
        // Same as validateCategoryAttributes but for update context
        if (CollectionUtils.isEmpty(attributes) && !CollectionUtils.isEmpty(categoryVersion.getAttributes())) {
            throw new BadRequestException("Category attributes are required");
        }

        Set<CategoryVersionAttribute> requiredAttributes = categoryVersion.getAttributes().stream()
                .filter(CategoryVersionAttribute::getIsRequired)
                .collect(Collectors.toSet());

        for (CategoryVersionAttribute required : requiredAttributes) {
            AttributeDefinition definition = required.getAttributeDefinition();
            boolean found = attributes.stream()
                    .anyMatch(attr -> attr.getAttributeDefinitionId().equals(definition.getAttrDefId()));

            if (!found) {
                throw new BadRequestException("Missing required attribute: " + definition.getName());
            }
        }

        // Validate attribute values
        for (AdAttributeRequest attr : attributes) {
            AttributeDefinition definition = categoryVersion.getAttributes().stream()
                    .filter(cva -> cva.getAttributeDefinition().getAttrDefId().equals(attr.getAttributeDefinitionId()))
                    .map(CategoryVersionAttribute::getAttributeDefinition)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Invalid attribute"));

            validateAttributeValue(attr, definition);
        }
    }

    private void validateBasicFields(AdvertisementCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().length() < 3) {
            throw new BadRequestException("Title must be at least 3 characters");
        }
        if (request.getTitle().length() > 100) {
            throw new BadRequestException("Title cannot exceed 100 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 2000) {
            throw new BadRequestException("Description cannot exceed 2000 characters");
        }
        if (request.getPrice() == null || request.getPrice().doubleValue() < 0) {
            throw new BadRequestException("Price must be non-negative");
        }
        if (request.getItemCondition() == null) {
            throw new BadRequestException("Item condition is required");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new BadRequestException("Address is required");
        }
    }

    private void validateCategory(AdvertisementCreateRequest request) {
        CategoryVersion categoryVersion = categoryVersioningService
                .getCategoryCurrentVersion(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Invalid category"));

        validateCategoryAttributes(request.getAttributes(), categoryVersion);
    }

    private void validateCategoryAttributes(Set<AdAttributeRequest> attributes, CategoryVersion categoryVersion) {
        if (CollectionUtils.isEmpty(attributes) && !CollectionUtils.isEmpty(categoryVersion.getAttributes())) {
            throw new BadRequestException("Category attributes are required");
        }

        Set<CategoryVersionAttribute> requiredAttributes = categoryVersion.getAttributes().stream()
                .filter(CategoryVersionAttribute::getIsRequired)
                .collect(Collectors.toSet());

        // Check required attributes
        for (CategoryVersionAttribute required : requiredAttributes) {
            AttributeDefinition definition = required.getAttributeDefinition();
            boolean found = attributes.stream()
                    .anyMatch(attr -> attr.getAttributeDefinitionId().equals(definition.getAttrDefId()));

            if (!found) {
                throw new BadRequestException("Missing required attribute: " + definition.getName());
            }
        }

        // Validate attribute values
        Set<Long> validAttrIds = categoryVersion.getAttributes().stream()
                .map(attr -> attr.getAttributeDefinition().getAttrDefId())
                .collect(Collectors.toSet());

        for (AdAttributeRequest attr : attributes) {
            if (!validAttrIds.contains(attr.getAttributeDefinitionId())) {
                throw new BadRequestException("Invalid attribute: " + attr.getAttributeDefinitionId());
            }

            AttributeDefinition definition = categoryVersion.getAttributes().stream()
                    .filter(cva -> cva.getAttributeDefinition().getAttrDefId().equals(attr.getAttributeDefinitionId()))
                    .map(CategoryVersionAttribute::getAttributeDefinition)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Attribute definition not found"));

            validateAttributeValue(attr, definition);
        }
    }

    private void validateAttributeValue(AdAttributeRequest attr, AttributeDefinition definition) {
        switch (definition.getDataType()) {
            case TEXT:
                if (definition.getIsRequired() && (attr.getTextValue() == null || attr.getTextValue().trim().isEmpty())) {
                    throw new BadRequestException("Text value required for: " + definition.getName());
                }
                break;
            case NUMBER:
                if (definition.getIsRequired() && attr.getNumericValue() == null) {
                    throw new BadRequestException("Numeric value required for: " + definition.getName());
                }
                break;
            case DATE:
                if (definition.getIsRequired() && attr.getDateValue() == null) {
                    throw new BadRequestException("Date value required for: " + definition.getName());
                }
                break;
            case LOCATION:
                if (definition.getIsRequired() && (attr.getLatitude() == null || attr.getLongitude() == null)) {
                    throw new BadRequestException("Location required for: " + definition.getName());
                }
                if (attr.getLatitude() != null) {
                    validateCoordinates(attr.getLatitude(), attr.getLongitude());
                }
                break;
        }
    }

    private void validateLocation(AdvertisementCreateRequest request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Invalid latitude value");
        }
        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Invalid longitude value");
        }
    }

    private void validatePackage(AdvertisementCreateRequest request) {
        AdPackage adPackage = adPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new BadRequestException("Invalid package"));

        if (!CollectionUtils.isEmpty(request.getMediaItems())
                && request.getMediaItems().size() > adPackage.getMaxMediaItems()) {
            throw new BadRequestException("Exceeds package media limit: " + adPackage.getMaxMediaItems());
        }
    }

    private void validateMedia(AdvertisementCreateRequest request) {
        if (!CollectionUtils.isEmpty(request.getMediaItems())) {
            for (MediaRequest media : request.getMediaItems()) {
                validateMediaFile(media.getFile());
            }
        }
    }

    private void validateMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Empty file provided");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid image type. Allowed: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }
    }

    private boolean containsFlaggedContent(String content) {
        String lowerContent = content.toLowerCase();
        return FLAGGED_WORDS.stream()
                .anyMatch(word -> lowerContent.contains(word.toLowerCase()));
    }

    public AdStatus validateContentAndGetStatus(AdvertisementCreateRequest request) {
        String contentToCheck = (request.getTitle() + " " +
                (request.getDescription() != null ? request.getDescription() : "")).toLowerCase();

        return containsFlaggedContent(contentToCheck) ?
                AdStatus.PENDING_REVIEW : AdStatus.ACTIVE;
    }
}