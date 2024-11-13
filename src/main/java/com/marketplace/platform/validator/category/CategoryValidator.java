package com.marketplace.platform.validator.category;

import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.category.CategoryAttribute;
import com.marketplace.platform.domain.category.CategoryStatus;
import com.marketplace.platform.domain.category.CategoryVersion;
import com.marketplace.platform.dto.request.CategoryCreateRequest;
import com.marketplace.platform.dto.request.CategoryUpdateRequest;
import com.marketplace.platform.dto.request.CategoryAttributeRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.category.CategoryRepository;
import com.marketplace.platform.repository.advertisement.AdvertisementRepository;
import com.marketplace.platform.repository.category.CategoryVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryValidator {
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;

    private final CategoryVersionRepository categoryVersionRepository;

    private static final int MAX_DEPTH = 5;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_ATTRIBUTES = 20;


    public void validateCreateRequest(CategoryCreateRequest request) {
        List<String> errors = new ArrayList<>();

        validateBasicFields(request.getName(), request.getDescription(), errors);
        validateNameUniqueness(request.getName(), null, errors);
        validateAttributes(request.getAttributes(), errors);

        throwIfErrors(errors);
    }


    public void validateUpdateRequest(Long categoryId, CategoryUpdateRequest request) {
        List<String> errors = new ArrayList<>();

        validateBasicFields(request.getName(), request.getDescription(), errors);
        validateNameUniqueness(request.getName(), categoryId, errors);
        validateAttributes(request.getAttributes(), errors);
        validateStatusUpdate(categoryId, request.getStatus(), errors);

        throwIfErrors(errors);
    }


    public void validateHierarchy(Category parent) {
        List<String> errors = new ArrayList<>();

        validateDepth(parent, errors);
        validateCircularReference(parent, errors);

        throwIfErrors(errors);
    }


    public void validateDeletion(Category category) {
        List<String> errors = new ArrayList<>();

        // Check for child categories
        if (category.hasChildren()) {
            errors.add("Cannot delete category with child categories. Please delete or move child categories first.");
        }

        // Check for active advertisements
        if (advertisementRepository.existsByCategoryIdAndStatusActive(category.getCategoryId())) {
            errors.add("Category has active advertisements. Please deactivate them first");
        }

        throwIfErrors(errors);

    }


    public void validateRestore(Category category) {
        List<String> errors = new ArrayList<>();

        if (category.getParent() != null &&
                category.getParent().getStatus() == CategoryStatus.DELETED) {
            errors.add("Cannot restore category because parent category is deleted");
        }

        CategoryVersion currentVersion = categoryVersionRepository
                .findCurrentVersion(category.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("No version found for category"));

        if (categoryVersionRepository.existsByNameIgnoreCaseAndCategoryIdNotAndStatus(
                currentVersion.getName(),
                category.getCategoryId(),
                CategoryStatus.ACTIVE)) {
            errors.add("Cannot restore category because another active category with the same name exists");
        }

        throwIfErrors(errors);
    }

    private void validateBasicFields(String name, String description, List<String> errors) {
        // Name validations
        if (name == null || name.trim().isEmpty()) {
            errors.add("Category name is required");
        } else {
            if (name.length() > MAX_NAME_LENGTH) {
                errors.add("Category name cannot exceed " + MAX_NAME_LENGTH + " characters");
            }
            if (!name.matches("^[a-zA-Z0-9\\s&-]+$")) {
                errors.add("Category name can only contain letters, numbers, spaces, &, and -");
            }
        }

        // Description validations
        if (description != null) {
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                errors.add("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
            }
        }
    }

    private void validateNameUniqueness(String name, Long excludeCategoryId, List<String> errors) {
        if (excludeCategoryId == null) {
            // For category creation
            if (categoryVersionRepository.existsByNameIgnoreCaseAndStatusNot(name, CategoryStatus.DELETED)) {
                errors.add("Category with this name already exists");
            }
            if (categoryVersionRepository.existsByNameIgnoreCaseAndStatus(name, CategoryStatus.DELETED)) {
                errors.add("A deleted category with this name exists. Please restore it or choose a different name");
            }
        } else {
            // For updates
            if (categoryVersionRepository.existsByNameIgnoreCaseAndCategoryIdNotAndStatus(
                    name, excludeCategoryId, CategoryStatus.ACTIVE)) {
                errors.add("Category with this name already exists");
            }
            if (categoryVersionRepository.existsByNameIgnoreCaseAndCategoryIdNotAndStatus(
                    name, excludeCategoryId, CategoryStatus.DELETED)) {
                errors.add("A deleted category with this name exists. Please restore it or choose a different name");
            }
        }
    }

    private void validateAttributes(Set<CategoryAttributeRequest> attributes, List<String> errors) {
        if (attributes != null) {
            if (attributes.size() > MAX_ATTRIBUTES) {
                errors.add("Maximum " + MAX_ATTRIBUTES + " attributes allowed per category");
            }

            Set<Long> attributeIds = new HashSet<>();
            Set<Integer> displayOrders = new HashSet<>();

            for (CategoryAttributeRequest attr : attributes) {
                // Check for duplicate attributes
                if (!attributeIds.add(attr.getAttributeDefinitionId())) {
                    errors.add("Duplicate attribute definitions are not allowed");
                }

                // Check for duplicate display orders
                if (attr.getDisplayOrder() != null && !displayOrders.add(attr.getDisplayOrder())) {
                    errors.add("Duplicate display orders are not allowed");
                }

                // Validate display order range
                if (attr.getDisplayOrder() != null &&
                        (attr.getDisplayOrder() < 1 || attr.getDisplayOrder() > MAX_ATTRIBUTES)) {
                    errors.add("Display order must be between 1 and " + MAX_ATTRIBUTES);
                }
            }
        }
    }

    private void validateStatusUpdate(Long categoryId, CategoryStatus newStatus, List<String> errors) {
        if (newStatus != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BadRequestException("Category not found"));

            // Cannot change status of deleted category
            if (category.getStatus() == CategoryStatus.DELETED) {
                errors.add("Cannot update status of deleted category");
            }

            // Validate transition to INACTIVE
            if (newStatus == CategoryStatus.INACTIVE) {
                // Check for active child categories
                if (categoryRepository.existsByParentIdAndStatusCustom(
                        categoryId, CategoryStatus.ACTIVE)) {
                    errors.add("Cannot deactivate category with active child categories");
                }

                // Check for active advertisements
                if (advertisementRepository.existsByCategoryIdAndStatusActive(categoryId)) {
                    errors.add("Cannot deactivate category with active advertisements");
                }
            }
        }
    }

    private void validateDepth(Category parent, List<String> errors) {
        if (parent.getDepth() >= MAX_DEPTH) {
            errors.add("Maximum category depth of " + MAX_DEPTH + " exceeded");
        }
    }

    private void validateCircularReference(Category parent, List<String> errors) {
        Category current = parent;
        while (current != null) {
            if (current.getParent() != null && current.getParent().equals(parent)) {
                errors.add("Circular reference detected in category hierarchy");
                break;
            }
            current = current.getParent();
        }
    }

    private void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            log.error("Validation errors: {}", errors);
            throw new BadRequestException(String.join(", ", errors));
        }
    }

    public void validateAttributeUpdates(
            Long categoryId,
            Set<CategoryAttributeRequest> newAttributes,
            Set<CategoryAttribute> existingAttributes) {

        List<String> errors = new ArrayList<>();

        // Get existing attributes map for comparison
        Map<Long, CategoryAttribute> existingAttributeMap = existingAttributes.stream()
                .collect(Collectors.toMap(
                        attr -> attr.getAttributeDefinition().getAttrDefId(),
                        attr -> attr
                ));

        // Check for active advertisements
        boolean hasActiveAds = advertisementRepository.existsByCategoryIdAndStatusActive(categoryId);

        if (hasActiveAds) {
            validateAttributeChangesWithActiveAds(
                    newAttributes,
                    existingAttributeMap,
                    errors
            );
        }

        // Validate removals
        validateAttributeRemovals(
                categoryId,
                newAttributes,
                existingAttributes,
                hasActiveAds,
                errors
        );

        // Validate new attributes
        validateNewAttributes(
                newAttributes,
                existingAttributeMap,
                errors
        );

        throwIfErrors(errors);
    }

    private void validateAttributeChangesWithActiveAds(
            Set<CategoryAttributeRequest> newAttributes,
            Map<Long, CategoryAttribute> existingAttributeMap,
            List<String> errors) {

        for (CategoryAttributeRequest attrRequest : newAttributes) {
            CategoryAttribute existingAttr = existingAttributeMap.get(
                    attrRequest.getAttributeDefinitionId()
            );

            if (existingAttr != null) {
                validateAttributeChange(existingAttr, attrRequest, errors);
            }
        }
    }

    private void validateAttributeChange(
            CategoryAttribute existing,
            CategoryAttributeRequest requested,
            List<String> errors) {

        // Validate requirement changes
        if (!Objects.equals(existing.getIsRequired(), requested.getIsRequired())) {
            if (existing.getIsRequired() && !requested.getIsRequired()) {
                // Changing from required to optional is always allowed
            } else if (!existing.getIsRequired() && requested.getIsRequired()) {
                // Changing from optional to required needs special handling
                errors.add(String.format(
                        "Changing attribute '%s' from optional to required may affect existing ads",
                        existing.getAttributeDefinition().getName()
                ));
            }
        }

        // Validate display order changes
        if (!Objects.equals(existing.getDisplayOrder(), requested.getDisplayOrder())) {
            // Display order changes are allowed but logged
            log.info("Display order change for attribute '{}' from {} to {}",
                    existing.getAttributeDefinition().getName(),
                    existing.getDisplayOrder(),
                    requested.getDisplayOrder()
            );
        }
    }

    private void validateAttributeRemovals(
            Long categoryId,
            Set<CategoryAttributeRequest> newAttributes,
            Set<CategoryAttribute> existingAttributes,
            boolean hasActiveAds,
            List<String> errors) {

        Set<Long> newAttrIds = newAttributes.stream()
                .map(CategoryAttributeRequest::getAttributeDefinitionId)
                .collect(Collectors.toSet());

        Set<CategoryAttribute> removedAttributes = existingAttributes.stream()
                .filter(attr -> !newAttrIds.contains(attr.getAttributeDefinition().getAttrDefId()))
                .collect(Collectors.toSet());

        if (!removedAttributes.isEmpty() && hasActiveAds) {
            // Check if any required attributes are being removed
            List<String> removedRequiredAttrs = removedAttributes.stream()
                    .filter(CategoryAttribute::getIsRequired)
                    .map(attr -> attr.getAttributeDefinition().getName())
                    .collect(Collectors.toList());

            if (!removedRequiredAttrs.isEmpty()) {
                errors.add(String.format(
                        "Cannot remove required attributes %s while ads are active",
                        String.join(", ", removedRequiredAttrs)
                ));
            }
        }
    }

    private void validateNewAttributes(
            Set<CategoryAttributeRequest> newAttributes,
            Map<Long, CategoryAttribute> existingAttributeMap,
            List<String> errors) {

        Set<Integer> displayOrders = new HashSet<>();

        for (CategoryAttributeRequest attr : newAttributes) {
            // Validate display order uniqueness
            if (attr.getDisplayOrder() != null && !displayOrders.add(attr.getDisplayOrder())) {
                errors.add(String.format(
                        "Duplicate display order %d found",
                        attr.getDisplayOrder()
                ));
            }

            // Validate new required attributes
            if (attr.getIsRequired() && !existingAttributeMap.containsKey(attr.getAttributeDefinitionId())) {
                errors.add(String.format(
                        "Adding new required attribute (ID: %d) may affect existing ads",
                        attr.getAttributeDefinitionId()
                ));
            }
        }
    }
}