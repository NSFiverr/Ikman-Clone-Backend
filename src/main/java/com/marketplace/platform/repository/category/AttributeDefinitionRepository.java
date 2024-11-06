package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    boolean existsByName(String name);

}
