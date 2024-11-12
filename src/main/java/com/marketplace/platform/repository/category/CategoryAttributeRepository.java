package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, CategoryAttribute.CategoryAttributeKey> {
    // Find all attributes for a category
    List<CategoryAttribute> findByCategoryCategoryId(Long categoryId);

    // Find all categories using a specific attribute definition
    List<CategoryAttribute> findByAttributeDefinitionAttrDefId(Long attrDefId);

    // Delete all attributes for a category
    @Modifying
    @Query("DELETE FROM CategoryAttribute ca WHERE ca.category.categoryId = :categoryId")
    void deleteByCategoryId(Long categoryId);

    // Delete a specific attribute from a category
    @Modifying
    @Query("DELETE FROM CategoryAttribute ca " +
            "WHERE ca.category.categoryId = :categoryId " +
            "AND ca.attributeDefinition.attrDefId = :attrDefId")
    void deleteByCategoryIdAndAttributeDefinitionId(Long categoryId, Long attrDefId);

    // Find required attributes for a category
    List<CategoryAttribute> findByCategoryCategoryIdAndIsRequiredTrue(Long categoryId);

    // Update display order for a category attribute
    @Modifying
    @Query("UPDATE CategoryAttribute ca SET ca.displayOrder = :displayOrder " +
            "WHERE ca.category.categoryId = :categoryId " +
            "AND ca.attributeDefinition.attrDefId = :attrDefId")
    void updateDisplayOrder(Long categoryId, Long attrDefId, Integer displayOrder);

    // Find attributes by display order range
    List<CategoryAttribute> findByCategoryCategoryIdAndDisplayOrderBetweenOrderByDisplayOrder(
            Long categoryId, Integer startOrder, Integer endOrder);

    // Check if an attribute is already assigned to a category
    boolean existsByCategoryCategoryIdAndAttributeDefinitionAttrDefId(Long categoryId, Long attrDefId);
}
