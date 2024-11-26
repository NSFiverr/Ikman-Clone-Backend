package com.marketplace.platform.service.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.platform.domain.category.*;
import com.marketplace.platform.dto.request.CategoryUpdateRequest;
import com.marketplace.platform.dto.request.CategoryAttributeRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.repository.category.CategoryVersionRepository;
import com.marketplace.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryVersioningService {

    private final CategoryVersionRepository categoryVersionRepository;
    private final AttributeDefinitionService attributeDefinitionService;
    private final ObjectMapper objectMapper;

    @Cacheable(cacheNames = "category_versions",
            key = "'v:' + #categoryId")
    public Optional<CategoryVersion> getCategoryCurrentVersion(Long categoryId) {
        return categoryVersionRepository.findCurrentVersion(categoryId);
    }


    @Cacheable(cacheNames = "category_versions",
            key = "'ve:' + #categoryId")
    public Optional<CategoryVersion> getCategoryCurrentVersionEager(Long categoryId) {
        return categoryVersionRepository.findTopByCategoryCategoryIdWithAttributesOrderByVersionNumberDesc(categoryId);
    }

    private CategoryVersionAttribute createVersionAttribute(CategoryAttributeRequest attrRequest) {
        AttributeDefinition attrDef = attributeDefinitionService
                .getAttributeDefinitionById(attrRequest.getAttributeDefinitionId());

        CategoryVersionAttribute attribute = new CategoryVersionAttribute();
        attribute.setAttributeDefinition(attrDef);
        attribute.setIsRequired(attrRequest.getIsRequired());
        attribute.setDisplayOrder(attrRequest.getDisplayOrder());
        attribute.setDefaultValue(attrRequest.getDefaultValue());
        attribute.setValidationRules(attrDef.getValidationRules());

        return attribute;
    }

    private CategoryVersionAttribute createVersionAttribute(
            CategoryAttributeRequest attrRequest,
            CategoryVersion categoryVersion) {

        AttributeDefinition attrDef = attributeDefinitionService
                .getAttributeDefinitionById(attrRequest.getAttributeDefinitionId());

        CategoryVersionAttribute attribute = new CategoryVersionAttribute();
        attribute.setAttributeDefinition(attrDef);
        attribute.setIsRequired(attrRequest.getIsRequired());
        attribute.setDisplayOrder(attrRequest.getDisplayOrder());
        attribute.setDefaultValue(attrRequest.getDefaultValue());
        attribute.setValidationRules(attrDef.getValidationRules());

        return attribute;
    }

    @CacheEvict(cacheNames = "category_versions", allEntries = true)
    @Transactional
    public CategoryVersion createNewVersion(Category category, CategoryUpdateRequest request) {
        // Close current version if exists
        Optional<CategoryVersion> currentVersion = getCategoryCurrentVersion(category.getCategoryId());
        currentVersion.ifPresent(version -> {
            version.setValidTo(LocalDateTime.now());
            categoryVersionRepository.save(version);
        });

        // Create new version
        CategoryVersion newVersion = new CategoryVersion();
        newVersion.setCategory(category);
        newVersion.setVersionNumber(currentVersion.map(v -> v.getVersionNumber() + 1).orElse(1));
        newVersion.setValidFrom(LocalDateTime.now());
        newVersion.setName(request.getName());
        newVersion.setDescription(request.getDescription());
        newVersion.setStatus(request.getStatus());

        // Create new attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            Set<CategoryVersionAttribute> versionAttributes = request.getAttributes().stream()
                    .map(this::createVersionAttribute)
                    .collect(Collectors.toSet());
            newVersion.setAttributes(versionAttributes);
        }

        return categoryVersionRepository.save(newVersion);
    }

    public CategoryVersion getCategoryVersionForAd(Long categoryId, LocalDateTime adCreationTime) {
        return categoryVersionRepository.findVersionAtTime(categoryId, adCreationTime)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No category version found for the specified time"));
    }

    public void validateVersionChange(Long categoryId) {
        List<CategoryVersion> versionsWithAds = categoryVersionRepository
                .findVersionsWithActiveAds(categoryId);

        if (!versionsWithAds.isEmpty()) {
            log.warn("Category {} has versions with active ads", categoryId);
        }

        if (categoryVersionRepository.hasChildCategories(categoryId)) {
            log.info("Category {} has child categories that may need version updates", categoryId);
        }
    }

    @Transactional
    public CategoryVersion createNewVersionWithNumber(
            Category category,
            CategoryUpdateRequest request,
            int versionNumber) {

        categoryVersionRepository.findCurrentVersion(category.getCategoryId()).ifPresent(currentVersion -> {
            currentVersion.setValidTo(LocalDateTime.now());
            categoryVersionRepository.save(currentVersion);
            log.debug("Closed version {} for category {}",
                    currentVersion.getVersionNumber(), category.getCategoryId());
        });

        // Create new version
        CategoryVersion newVersion = new CategoryVersion();
        newVersion.setCategory(category);
        newVersion.setName(request.getName());
        newVersion.setDescription(request.getDescription());
        newVersion.setStatus(request.getStatus());
        newVersion.setValidFrom(LocalDateTime.now());
        newVersion.setVersionNumber(versionNumber);

        // Set attributes if any
        if (request.getAttributes() != null) {
            Set<CategoryVersionAttribute> attributes = request.getAttributes().stream()
                    .map(attr -> createVersionAttribute(attr, newVersion))
                    .collect(Collectors.toSet());
            newVersion.setAttributes(attributes);
        }

        return categoryVersionRepository.save(newVersion);
    }
}