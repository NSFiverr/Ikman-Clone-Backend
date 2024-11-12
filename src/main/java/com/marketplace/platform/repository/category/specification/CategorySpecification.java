package com.marketplace.platform.repository.category.specification;

import com.marketplace.platform.domain.category.Category;
import com.marketplace.platform.domain.category.CategoryStatus;
import com.marketplace.platform.dto.request.CategorySearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CategorySpecification {

    public static Specification<Category> withCriteria(CategorySearchCriteria criteria) {
        return (Root<Category> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!query.getResultType().equals(Long.class)) {
                addPredicates(criteria, root, query, criteriaBuilder, predicates);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addPredicates(CategorySearchCriteria criteria,
                                      Root<Category> root,
                                      CriteriaQuery<?> query,
                                      CriteriaBuilder criteriaBuilder,
                                      List<Predicate> predicates) {
        if (criteria != null) {
            // Name search (partial match)
            if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + criteria.getName().toLowerCase().trim() + "%"
                ));
            }

            // Parent ID
            if (criteria.getParentId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("parent").get("categoryId"),
                        criteria.getParentId()
                ));
            }

            // Status
            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        criteria.getStatus()
                ));
            }

            // Depth
            if (criteria.getDepth() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("depth"),
                        criteria.getDepth()
                ));
            }

            // Include/Exclude deleted
            if (!criteria.getIncludeDeleted()) {
                predicates.add(criteriaBuilder.notEqual(
                        root.get("status"),
                        CategoryStatus.DELETED
                ));
            }

            // Date range
            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                predicates.add(criteriaBuilder.between(
                        root.get("createdAt"),
                        criteria.getStartDate(),
                        criteria.getEndDate()
                ));
            }

            // Minimum attribute count
            if (criteria.getMinAttributeCount() != null) {
                Join<Object, Object> attributesJoin = root.join("attributes", JoinType.LEFT);
                predicates.add(criteriaBuilder.ge(
                        criteriaBuilder.count(attributesJoin),
                        criteria.getMinAttributeCount()
                ));
                query.groupBy(root);
            }

            // Attribute definitions
            if (criteria.getAttributeDefinitionIds() != null && !criteria.getAttributeDefinitionIds().isEmpty()) {
                Join<Object, Object> attributesJoin = root.join("attributes", JoinType.INNER);
                Join<Object, Object> attrDefJoin = attributesJoin.join("attributeDefinition");
                predicates.add(attrDefJoin.get("attrDefId").in(criteria.getAttributeDefinitionIds()));
            }

            // Active advertisements
            if (criteria.getHasActiveAds() != null) {
                Join<Object, Object> adsJoin = root.join("advertisements", JoinType.LEFT);
                if (criteria.getHasActiveAds()) {
                    predicates.add(criteriaBuilder.equal(adsJoin.get("status"), "ACTIVE"));
                } else {
                    predicates.add(criteriaBuilder.isNull(adsJoin));
                }
            }
        }
    }
}