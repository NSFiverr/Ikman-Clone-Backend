package com.marketplace.platform.service.category;

import com.marketplace.platform.domain.category.*;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.mapper.CategoryMapper;
import com.marketplace.platform.repository.category.CategoryRepository;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.repository.category.CategoryVersionRepository;
import com.marketplace.platform.repository.category.specification.CategorySpecification;
import com.marketplace.platform.validator.category.CategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = "categories")
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryVersionRepository categoryVersionRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryValidator categoryValidator;
    private final CategoryVersioningService categoryVersioningService;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        categoryValidator.validateCreateRequest(request);

        Category category = createCategoryEntity(request);

        CategoryVersion initialVersion = categoryVersioningService.createNewVersion(category, categoryMapper.toUpdateRequest(request));

        return categoryMapper.toResponse(category, initialVersion);
    }

    private Category createCategoryEntity(CategoryCreateRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            categoryValidator.validateHierarchy(parent);
        }

        Category category = categoryMapper.toEntity(request);
        category.setParent(parent);

        // TODO: Replace with actual user from security context
        User dummyUser = new User();
        dummyUser.setUserId(1L);
        category.setCreatedBy(dummyUser);

        category.setStatus(CategoryStatus.ACTIVE);

        return categoryRepository.save(category);
    }

    @Override
    @Cacheable(key = "#id")
    public CategoryResponse getCategory(Long id) {
        Category category = findCategory(id);
        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new ResourceNotFoundException("Category not found");
        }

        // Get current version of the category
        CategoryVersion currentVersion = categoryVersioningService
                .getCategoryCurrentVersion(id)
                .orElseThrow(() -> new ResourceNotFoundException("No active version found for category"));

        return categoryMapper.toResponse(category, currentVersion);
    }

    @Override
    @Cacheable(key = "'all:' + #criteria + ':' + #pageable")
    public Page<CategoryResponse> getAllCategories(CategorySearchCriteria criteria, Pageable pageable) {
        Specification<Category> spec = CategorySpecification.withCriteria(criteria);
        Page<Category> categories = categoryRepository.findAll(spec, pageable);

        return categories.map(category -> {
            CategoryVersion currentVersion = categoryVersioningService
                    .getCategoryCurrentVersion(category.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("No active version found for category"));
            return categoryMapper.toResponse(category, currentVersion);
        });
    }

    @Override
    @Transactional
    @CacheEvict(key = "#id")
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = findCategory(id);

        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new BadRequestException("Cannot update deleted category");
        }

        categoryValidator.validateUpdateRequest(id, request);

        // Create new version with updates
        CategoryVersion newVersion = categoryVersioningService.createNewVersion(category, request);

        // Update basic category information
        updateBasicInfo(category, request);
        category = categoryRepository.save(category);

        return categoryMapper.toResponse(category, newVersion);
    }

    @Override
    @Transactional
    @CacheEvict(key = "#id")
    public void deleteCategory(Long id) {
        Category category = findCategory(id);

        if (category.getStatus() == CategoryStatus.DELETED) {
            throw new BadRequestException("Category is already deleted");
        }

        categoryValidator.validateDeletion(category);

        // Close current version
        categoryVersioningService.getCategoryCurrentVersion(id).ifPresent(version -> {
            version.setValidTo(LocalDateTime.now());
            categoryVersionRepository.save(version);
        });

        // Archive the category
        category.setStatus(CategoryStatus.DELETED);
        categoryRepository.save(category);

        log.info("Category with id {} has been soft deleted", id);
    }

    @Override
    @Transactional
    @CacheEvict(key = "#id")
    public CategoryResponse restoreCategory(Long id) {
        Category category = findCategory(id);

        if (category.getStatus() != CategoryStatus.DELETED) {
            throw new BadRequestException("Category is not deleted");
        }

        categoryValidator.validateRestore(category);

        category.setStatus(CategoryStatus.INACTIVE); // Start as inactive to review
        category = categoryRepository.save(category);

        // Create new version for restored category
        CategoryVersion lastVersion = categoryVersionRepository
                .findByCategoryIdOrderByVersionNumberDesc(id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No previous version found"));

        CategoryUpdateRequest request = categoryMapper.toUpdateRequest(lastVersion);
        CategoryVersion newVersion = categoryVersioningService.createNewVersion(category, request);

        return categoryMapper.toResponse(category, newVersion);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private void updateBasicInfo(Category category, CategoryUpdateRequest request) {
        if (request.getStatus() != null && request.getStatus() != CategoryStatus.DELETED) {
            category.setStatus(request.getStatus());
        }
    }
}