package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, CategoryAttribute.CategoryAttributeKey> {
    void deleteByCategoryCategoryId(Long categoryId);
}

