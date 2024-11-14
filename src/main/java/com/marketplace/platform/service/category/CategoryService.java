package com.marketplace.platform.service.category;

import com.marketplace.platform.dto.request.*;
import com.marketplace.platform.dto.response.AttributeDefinitionResponse;
import com.marketplace.platform.dto.response.CategoryResponse;
import com.marketplace.platform.dto.response.CategoryStatistics;
import com.marketplace.platform.repository.category.CategoryStatsProjection;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CategoryService {
    // Core CRUD operations
    CategoryResponse createCategory(CategoryCreateRequest request);
    CategoryResponse getCategory(Long id);
    Page<CategoryResponse> getAllCategories(CategorySearchCriteria criteria, Pageable pageable);

    Page<CategoryResponse> getCategoryVersionHistory(Long categoryId, Pageable pageable);

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);
    void deleteCategory(Long id);
    CategoryResponse restoreCategory(Long id);

}