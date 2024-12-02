package com.marketplace.platform.repository.advertisement;

import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.dto.request.AdvertisementSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdvertisementSearchSpecification {

    public Specification<Advertisement> buildSpecification(AdvertisementSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by active status
            predicates.add(cb.equal(root.get("status"), AdStatus.ACTIVE));

            // Basic filters
            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryVersion").get("category").get("categoryId"),
                        criteria.getCategoryId()));
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
            }

            if (criteria.getCondition() != null) {
                predicates.add(cb.equal(root.get("itemCondition"), criteria.getCondition()));
            }

            // Location-based filtering
            if (criteria.getLatitude() != null && criteria.getLongitude() != null && criteria.getRadius() != null) {
                // Using PostGIS ST_DWithin for efficient radius search
                String pointWkt = String.format("POINT(%f %f)", criteria.getLongitude(), criteria.getLatitude());
                predicates.add(cb.isTrue(cb.function("ST_DWithin",
                        Boolean.class,
                        root.get("locationCoordinates"),
                        cb.function("ST_GeomFromText", Object.class, cb.literal(pointWkt)),
                        cb.literal(criteria.getRadius() * 1000))));
            }

            // Handle expired featured/top ads
            LocalDateTime now = LocalDateTime.now();
            if (criteria.getFeatured() != null && criteria.getFeatured()) {
                predicates.add(cb.and(
                        cb.isTrue(root.get("isFeatured")),
                        cb.or(
                                cb.isNull(root.get("featuredUntil")),
                                cb.greaterThan(root.get("featuredUntil"), now)
                        )
                ));
            }

            if (criteria.getTopAdsOnly() != null && criteria.getTopAdsOnly()) {
                predicates.add(cb.and(
                        cb.isTrue(root.get("isTopAd")),
                        cb.or(
                                cb.isNull(root.get("topAdUntil")),
                                cb.greaterThan(root.get("topAdUntil"), now)
                        )
                ));
            }

            // Ensure not expired
            predicates.add(cb.or(
                    cb.isNull(root.get("expiresAt")),
                    cb.greaterThan(root.get("expiresAt"), now)
            ));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
