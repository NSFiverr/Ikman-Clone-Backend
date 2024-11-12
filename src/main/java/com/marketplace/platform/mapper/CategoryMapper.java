package com.marketplace.platform.mapper;

import com.marketplace.platform.domain.category.*;
import com.marketplace.platform.dto.request.CategoryCreateRequest;
import com.marketplace.platform.dto.request.CategoryUpdateRequest;
import com.marketplace.platform.dto.request.CategoryAttributeRequest;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.dto.response.CategoryAttributeResponse;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return category;
    }

    public CategoryResponse toResponse(Category category, CategoryVersion version) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(version.getName())  // Use version name instead of category name
                .description(version.getDescription())  // Use version description
                .treePath(category.getTreePath())
                .depth(category.getDepth())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .validFrom(version.getValidFrom())
                .validTo(version.getValidTo())
                .attributes(mapVersionAttributes(version.getAttributes()))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public CategoryUpdateRequest toUpdateRequest(CategoryCreateRequest createRequest) {
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest();
        updateRequest.setName(createRequest.getName());
        updateRequest.setDescription(createRequest.getDescription());
        updateRequest.setStatus(CategoryStatus.ACTIVE); // Default status for new categories
        updateRequest.setAttributes(createRequest.getAttributes()); // Assuming attributes structure is the same
        return updateRequest;
    }

    public CategoryUpdateRequest toUpdateRequest(CategoryVersion version) {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName(version.getName());
        request.setDescription(version.getDescription());
        request.setStatus(version.getStatus());
        request.setAttributes(mapToAttributeRequests(version.getAttributes()));
        return request;
    }

    private Set<CategoryAttributeResponse> mapVersionAttributes(Set<CategoryVersionAttribute> attributes) {
        return attributes.stream()
                .map(this::mapVersionAttributeResponse)
                .collect(Collectors.toSet());
    }

    private CategoryAttributeResponse mapVersionAttributeResponse(CategoryVersionAttribute attribute) {
        return CategoryAttributeResponse.builder()
                .attributeDefinitionId(attribute.getAttributeDefinition().getAttrDefId())
                .attributeName(attribute.getAttributeDefinition().getName())
                .displayName(attribute.getAttributeDefinition().getDisplayName())
                .isRequired(attribute.getIsRequired())
                .displayOrder(attribute.getDisplayOrder())
                .defaultValue(attribute.getDefaultValue())
                .validationRules(attribute.getValidationRules())
                .build();
    }

    private Set<CategoryAttributeRequest> mapToAttributeRequests(Set<CategoryVersionAttribute> attributes) {
        return attributes.stream()
                .map(this::mapToAttributeRequest)
                .collect(Collectors.toSet());
    }

    private CategoryAttributeRequest mapToAttributeRequest(CategoryVersionAttribute attribute) {
        return CategoryAttributeRequest.builder()
                .attributeDefinitionId(attribute.getAttributeDefinition().getAttrDefId())
                .isRequired(attribute.getIsRequired())
                .displayOrder(attribute.getDisplayOrder())
                .defaultValue(attribute.getDefaultValue())
                .validationRules(attribute.getValidationRules())
                .build();
    }
}