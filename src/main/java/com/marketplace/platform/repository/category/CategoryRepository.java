package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.category.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByParentId(Long parentId);
    boolean existsByParentIdAndStatus(Long parentId, CategoryStatus status);
    boolean existsByNameIgnoreCaseAndStatusNot(String name, CategoryStatus status);
    boolean existsByNameIgnoreCaseAndStatus(String name, CategoryStatus status);
    boolean existsByNameIgnoreCaseAndCategoryIdNotAndStatusNot(String name, Long categoryId, CategoryStatus status);
    boolean existsByNameIgnoreCaseAndCategoryIdNotAndStatus(String name, Long categoryId, CategoryStatus status);





}