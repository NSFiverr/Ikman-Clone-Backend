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
    boolean existsByParentCategoryId(Long parentId);

    boolean existsByParentCategoryIdAndStatus(Long parentId, CategoryStatus status);
    boolean existsByCategoryIdNotAndStatus(Long categoryId, CategoryStatus status);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
            "WHERE c.parent.categoryId = :parentId")
    boolean existsByParentId(@Param("parentId") Long parentId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
            "WHERE c.parent.categoryId = :parentId AND c.status = :status")
    boolean existsByParentIdAndStatusCustom(
            @Param("parentId") Long parentId,
            @Param("status") CategoryStatus status
    );






}