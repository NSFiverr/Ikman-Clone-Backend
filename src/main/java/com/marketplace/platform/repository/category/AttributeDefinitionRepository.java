package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.AttributeDefinition;
import com.marketplace.platform.domain.category.FieldType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    // Find attribute definitions by data type
    List<AttributeDefinition> findByDataType(FieldType dataType);

    // Find searchable attribute definitions
    List<AttributeDefinition> findByIsSearchableTrue();

    // Find required attribute definitions
    List<AttributeDefinition> findByIsRequiredTrue();

    // Find by name
    Optional<AttributeDefinition> findByNameIgnoreCase(String name);

    // Check if attribute definition name exists
    boolean existsByNameIgnoreCase(String name);

    // Find attribute definitions used in a specific category
    @Query("SELECT ad FROM AttributeDefinition ad " +
            "JOIN ad.categoryAttributes ca " +
            "WHERE ca.category.categoryId = :categoryId")
    List<AttributeDefinition> findByCategoryId(Long categoryId);

    // Find unused attribute definitions
    @Query("SELECT ad FROM AttributeDefinition ad " +
            "WHERE NOT EXISTS (SELECT ca FROM CategoryAttribute ca WHERE ca.attributeDefinition = ad)")
    List<AttributeDefinition> findUnusedAttributeDefinitions();
}