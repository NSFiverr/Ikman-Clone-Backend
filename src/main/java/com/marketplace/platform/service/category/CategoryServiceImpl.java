    package com.marketplace.platform.service.category;

    import com.marketplace.platform.domain.admin.Admin;
    import com.marketplace.platform.domain.category.*;
    import com.marketplace.platform.domain.user.User;
    import com.marketplace.platform.dto.request.*;
    import com.marketplace.platform.dto.response.CategoryResponse;
    import com.marketplace.platform.exception.BadRequestException;
    import com.marketplace.platform.mapper.CategoryMapper;
    import com.marketplace.platform.repository.admin.AdminRepository;
    import com.marketplace.platform.repository.category.CategoryRepository;
    import com.marketplace.platform.exception.ResourceNotFoundException;
    import com.marketplace.platform.repository.category.CategoryVersionRepository;
    import com.marketplace.platform.repository.category.specification.CategorySpecification;
    import com.marketplace.platform.service.auth.JwtService;
    import com.marketplace.platform.validator.category.CategoryValidator;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.cache.annotation.CacheEvict;
    import org.springframework.cache.annotation.Cacheable;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.LocalDateTime;
    import java.util.Comparator;
    import java.util.Optional;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class CategoryServiceImpl implements CategoryService {
        private final CategoryRepository categoryRepository;
        private final CategoryVersionRepository categoryVersionRepository;
        private final CategoryMapper categoryMapper;
        private final CategoryValidator categoryValidator;
        private final CategoryVersioningService categoryVersioningService;

        private final JwtService jwtService;


        @Override
        @Transactional
        @CacheEvict(cacheNames = {"categories", "category_lists", "category_versions"}, allEntries = true)
        public CategoryResponse createCategory(CategoryCreateRequest request, String token) {
            categoryValidator.validateCreateRequest(request);

            Category category = createCategoryEntity(request, token);

            CategoryVersion initialVersion = categoryVersioningService.createNewVersion(category, categoryMapper.toUpdateRequest(request));

            return categoryMapper.toResponse(category, initialVersion);
        }

        private Category createCategoryEntity(CategoryCreateRequest request, String token) {
            Category parent = null;
            if (request.getParentId() != null) {
                parent = categoryRepository.findById(request.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
                categoryValidator.validateHierarchy(parent);
            }

            Category category = categoryMapper.toEntity(request);
            category.setParent(parent);

            Optional<Admin> optionalAdmin = jwtService.getAdminFromToken(token);

            optionalAdmin.ifPresentOrElse(
                    admin -> category.setCreatedBy(admin),
                    () -> { // Handle the case where the admin is not found
                        log.error("Admin not found in the repository for the given access token");
                        throw new ResourceNotFoundException("Admin not found");
                    }
            );


            category.setStatus(CategoryStatus.ACTIVE);

            return categoryRepository.save(category);
        }

        @Override
        @Cacheable(cacheNames = "categories", key = "#id")
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
        @Cacheable(cacheNames = "category_lists",
                key = "T(java.util.Objects).hash(#criteria, #pageable)")
        public Page<CategoryResponse> getAllCategories(CategorySearchCriteria criteria, Pageable pageable) {
            Specification<Category> spec = CategorySpecification.withCriteria(criteria);

            Page<Category> categories = categoryRepository.findAll(spec, pageable);

            return categories.map(category -> {
                try {
                    CategoryVersion currentVersion = categoryVersioningService
                            .getCategoryCurrentVersionEager(category.getCategoryId())
                            .orElseThrow(() -> new ResourceNotFoundException("No active version found for category"));
                    return categoryMapper.toResponse(category, currentVersion);
                } catch (Exception e) {
                    log.error("Error getting current version for category {}: {}", category.getCategoryId(), e.getMessage());
                    throw new RuntimeException("Error processing category response", e);
                }
            });
        }

        @Override
        @Cacheable(cacheNames = "category_lists",
                key = "'deleted:' + T(java.util.Objects).hash(#pageable)")
        public Page<CategoryResponse> getDeletedCategories(Pageable pageable) {
            Page<Category> deletedCategories = categoryRepository.findByLatestVersionStatus(
                    CategoryStatus.DELETED,
                    pageable
            );

            return deletedCategories.map(category -> {
                CategoryVersion latestVersion = category.getVersions().stream()
                        .max(Comparator.comparing(CategoryVersion::getVersionNumber))
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No version found for category: " + category.getCategoryId()
                        ));
                return categoryMapper.toResponse(category, latestVersion);
            });
        }

        @Override
        @Cacheable(cacheNames = "category_versions",
                key = "'history:' + #categoryId + ':' + #pageable")
        public Page<CategoryResponse> getCategoryVersionHistory(Long categoryId, Pageable pageable) {
            // First verify the category exists
            Category category = findCategory(categoryId);
            if (category.getStatus() == CategoryStatus.DELETED) {
                throw new ResourceNotFoundException("Category not found");
            }

            // Get all versions paginated
            Page<CategoryVersion> versions = categoryVersionRepository.findVersionHistoryByCategory(
                    categoryId,
                    pageable
            );

            return versions.map(version -> categoryMapper.toResponse(category, version));
        }

        @Override
        @Transactional
        @CacheEvict(cacheNames = {"categories", "category_lists", "category_versions"},
                key = "#id", allEntries = true)
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
        @CacheEvict(cacheNames = {"categories", "category_lists", "category_versions"},
                key = "#id", allEntries = true)
        public void deleteCategory(Long id) {
            Category category = findCategory(id);

            if (category.getStatus() == CategoryStatus.DELETED) {
                throw new BadRequestException("Category is already deleted");
            }

            categoryValidator.validateDeletion(category);

            // Close current version
            categoryVersioningService.getCategoryCurrentVersion(id).ifPresent(version -> {
                version.setValidTo(LocalDateTime.now());
                version.setStatus(CategoryStatus.DELETED);
                categoryVersionRepository.save(version);
            });

            // Archive the category
            category.setStatus(CategoryStatus.DELETED);
            category.setUpdatedAt(LocalDateTime.now());
            categoryRepository.save(category);

            log.info("Category with id {} has been soft deleted", id);
        }

        @Override
        @Transactional
        @CacheEvict(cacheNames = {"categories", "category_lists", "category_versions"},
                key = "#id", allEntries = true)
        public CategoryResponse restoreCategory(Long id) {
            Category category = findCategory(id);

            if (category.getStatus() != CategoryStatus.DELETED) {
                throw new BadRequestException("Category is not deleted");
            }

            categoryValidator.validateRestore(category);

            // Get the last active version before deletion
            CategoryVersion lastVersion = categoryVersionRepository
                    .findVersionsForCategory(id)
                    .stream()
                    .filter(v -> v.getValidTo() != null)  // Get only closed versions
                    .max(Comparator.comparing(CategoryVersion::getVersionNumber))
                    .orElseThrow(() -> new ResourceNotFoundException("No previous version found"));

            // Get max version number to ensure uniqueness
            Integer maxVersionNumber = categoryVersionRepository
                    .findMaxVersionNumberForCategory(id)
                    .orElse(0);

            category.setStatus(CategoryStatus.ACTIVE);
            category = categoryRepository.save(category);


            CategoryUpdateRequest request = categoryMapper.toUpdateRequest(lastVersion);
            request.setStatus(CategoryStatus.ACTIVE);
            CategoryVersion newVersion = categoryVersioningService.createNewVersionWithNumber(
                    category,
                    request,
                    maxVersionNumber + 1
            );

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

        @Scheduled(fixedRate = 3600000) // Every hour
        @CacheEvict(cacheNames = {"categories", "category_lists", "category_versions"},
                allEntries = true)
        public void clearAllCaches() {
            log.info("Clearing all category related caches");
        }
    }