package com.marketplace.platform.repository.advertisement;

import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.advertisement.ItemCondition;
import com.marketplace.platform.dto.projection.AdvertisementListingProjection;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AdvertisementRepository extends
        JpaRepository<Advertisement, Long>,
        JpaSpecificationExecutor<Advertisement> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Advertisement a " +
            "WHERE a.categoryVersion.category.categoryId = :categoryId " +
            "AND a.status = 'ACTIVE'")
    boolean existsByCategoryIdAndStatusActive(@Param("categoryId") Long categoryId);


    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Advertisement a " +
            "WHERE a.categoryVersion.id = :categoryVersionId " +
            "AND a.status = 'ACTIVE'")
    boolean existsByCategoryVersionIdAndStatusActive(@Param("categoryVersionId") Long categoryVersionId);



    @QueryHints({
            @QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "50")
    })
    @Query("""
        SELECT 
            a.adId as adId,
            a.title as title,
            a.price as price,
            a.address as address,
            a.itemCondition as itemCondition,
            a.isNegotiable as isNegotiable,
            a.isFeatured as isFeatured,
            a.isTopAd as isTopAd,
            a.viewCount as viewCount,
            a.createdAt as createdAt,
            a.expiresAt as expiresAt,
            m.firebaseUrl as thumbnailUrl,
            cv.category.categoryId as categoryVersionCategoryId,
            cv.name as categoryVersionName,
            cv.versionNumber as categoryVersionNumber,
            u.userId as userUserId,
            u.firstName as userFirstName,
            u.lastName as userLastName,
            u.isEmailVerified as userIsEmailVerified
        FROM Advertisement a
        JOIN a.categoryVersion cv
        JOIN a.user u
        LEFT JOIN a.mediaItems m
        WHERE a.status = 'ACTIVE'
        AND (m IS NULL OR (m.displayOrder = 0 AND m.mediaType = 'IMAGE'))
        AND (:categoryId IS NULL OR cv.category.categoryId = :categoryId)
        AND (:minPrice IS NULL OR a.price >= :minPrice)
        AND (:maxPrice IS NULL OR a.price <= :maxPrice)
        AND (:condition IS NULL OR a.itemCondition = :condition)
        AND ((:includeFeatured = false) OR (a.isFeatured = true AND (a.featuredUntil IS NULL OR a.featuredUntil > CURRENT_TIMESTAMP)))
        AND ((:includeTopAds = false) OR (a.isTopAd = true AND (a.topAdUntil IS NULL OR a.topAdUntil > CURRENT_TIMESTAMP)))
        AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY
            CASE WHEN :includeFeatured = true AND a.isFeatured = true THEN 1 ELSE 0 END DESC,
            CASE WHEN :includeTopAds = true AND a.isTopAd = true THEN 1 ELSE 0 END DESC,
            a.createdAt DESC
        """)
    Page<AdvertisementListingProjection> findActiveAdvertisementsForListing(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") ItemCondition condition,
            @Param("includeFeatured") boolean includeFeatured,
            @Param("includeTopAds") boolean includeTopAds,
            Pageable pageable
    );

    @Query(value = """
    SELECT 
        a.ad_id as adId,
        a.title as title,
        a.price as price,
        a.address as address,
        a.item_condition as itemCondition,
        a.is_negotiable as isNegotiable,
        a.is_featured as isFeatured,
        a.is_top_ad as isTopAd,
        a.view_count as viewCount,
        a.created_at as createdAt,
        a.expires_at as expiresAt,
        u.user_id as userUserId,
        u.first_name as userFirstName,
        u.last_name as userLastName,
        u.is_email_verified as userIsEmailVerified,
        cv.category_id as categoryVersionCategoryId,
        cv.name as categoryVersionName,
        cv.version_number as categoryVersionNumber,
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'firebaseUrl', m2.firebase_url,
                    'displayOrder', m2.display_order,
                    'mediaType', m2.media_type
                )
            )
            FROM (
                SELECT firebase_url, display_order, media_type
                FROM ad_media m2
                WHERE m2.ad_id = a.ad_id 
                AND m2.media_type = 'IMAGE'
                ORDER BY m2.display_order
            ) m2
        ) as mediaItems
    FROM advertisements a
    JOIN users u ON a.user_id = u.user_id
    JOIN category_versions cv ON a.category_version_id = cv.id
    WHERE a.ad_status = 'ACTIVE'
    AND ST_Distance_Sphere(
        location_coordinates,
        ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'))
    ) <= :radiusInMeters
    AND (:categoryId IS NULL OR cv.category_id = :categoryId)
    AND (:categoryIds IS NULL OR cv.category_id IN (:categoryIds))
    AND (:minPrice IS NULL OR a.price >= :minPrice)
    AND (:maxPrice IS NULL OR a.price <= :maxPrice)
    AND (:condition IS NULL OR a.item_condition = :condition)
    AND (:verifiedSellersOnly IS NULL OR :verifiedSellersOnly = FALSE OR u.is_email_verified = TRUE)
    AND (:negotiable IS NULL OR :negotiable = FALSE OR a.is_negotiable = TRUE)
    AND (:hasPhotos IS NULL OR :hasPhotos = FALSE OR EXISTS (
        SELECT 1 FROM ad_media m 
        WHERE m.ad_id = a.ad_id AND m.media_type = 'IMAGE'
    ))
    AND (:featured IS NULL OR :featured = FALSE OR a.is_featured = TRUE)
    AND (:topAdsOnly IS NULL OR :topAdsOnly = FALSE OR a.is_top_ad = TRUE)
    AND (:sellerId IS NULL OR a.user_id = :sellerId)
    AND (:minViewCount IS NULL OR a.view_count >= :minViewCount)
    AND (:postedAfter IS NULL OR a.created_at >= :postedAfter)
    AND (:postedBefore IS NULL OR a.created_at <= :postedBefore)
    AND (:excludeAdIds IS NULL OR a.ad_id NOT IN (:excludeAdIds))
    AND (:searchTerm IS NULL OR 
        (a.title LIKE CONCAT('%', :searchTerm, '%') OR 
         a.description LIKE CONCAT('%', :searchTerm, '%'))
    )
    ORDER BY
        CASE WHEN :sortBy = 'price' AND :sortDirection = 'ASC' THEN a.price END ASC,
        CASE WHEN :sortBy = 'price' AND :sortDirection = 'DESC' THEN a.price END DESC,
        CASE WHEN :sortBy = 'viewCount' AND :sortDirection = 'ASC' THEN a.view_count END ASC,
        CASE WHEN :sortBy = 'viewCount' AND :sortDirection = 'DESC' THEN a.view_count END DESC,
        CASE WHEN :sortBy = 'title' AND :sortDirection = 'ASC' THEN a.title END ASC,
        CASE WHEN :sortBy = 'title' AND :sortDirection = 'DESC' THEN a.title END DESC,
        CASE WHEN :sortBy = 'createdAt' OR :sortBy IS NULL THEN a.created_at END DESC
    """,
            countQuery = """
        SELECT COUNT(*)
        FROM advertisements a
        JOIN users u ON a.user_id = u.user_id
        JOIN category_versions cv ON a.category_version_id = cv.id
        WHERE a.ad_status = 'ACTIVE'
        AND ST_Distance_Sphere(
            location_coordinates,
            ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'))
        ) <= :radiusInMeters
        AND (:categoryId IS NULL OR cv.category_id = :categoryId)
        AND (:categoryIds IS NULL OR cv.category_id IN (:categoryIds))
        AND (:minPrice IS NULL OR a.price >= :minPrice)
        AND (:maxPrice IS NULL OR a.price <= :maxPrice)
        AND (:condition IS NULL OR a.item_condition = :condition)
        AND (:verifiedSellersOnly IS NULL OR :verifiedSellersOnly = FALSE OR u.is_email_verified = TRUE)
        AND (:negotiable IS NULL OR :negotiable = FALSE OR a.is_negotiable = TRUE)
        AND (:hasPhotos IS NULL OR :hasPhotos = FALSE OR EXISTS (
            SELECT 1 FROM ad_media m 
            WHERE m.ad_id = a.ad_id AND m.media_type = 'IMAGE'
        ))
        AND (:featured IS NULL OR :featured = FALSE OR a.is_featured = TRUE)
        AND (:topAdsOnly IS NULL OR :topAdsOnly = FALSE OR a.is_top_ad = TRUE)
        AND (:sellerId IS NULL OR a.user_id = :sellerId)
        AND (:minViewCount IS NULL OR a.view_count >= :minViewCount)
        AND (:postedAfter IS NULL OR a.created_at >= :postedAfter)
        AND (:postedBefore IS NULL OR a.created_at <= :postedBefore)
        AND (:excludeAdIds IS NULL OR a.ad_id NOT IN (:excludeAdIds))
        AND (:searchTerm IS NULL OR 
            (a.title LIKE CONCAT('%', :searchTerm, '%') OR 
             a.description LIKE CONCAT('%', :searchTerm, '%'))
        )
    """,
            nativeQuery = true)
    Page<AdvertisementListingProjection> findNearbyAdvertisements(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusInMeters") Double radiusInMeters,
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") String condition,
            @Param("verifiedSellersOnly") Boolean verifiedSellersOnly,
            @Param("negotiable") Boolean negotiable,
            @Param("hasPhotos") Boolean hasPhotos,
            @Param("featured") Boolean featured,
            @Param("topAdsOnly") Boolean topAdsOnly,
            @Param("sellerId") Long sellerId,
            @Param("minViewCount") Integer minViewCount,
            @Param("postedAfter") LocalDateTime postedAfter,
            @Param("postedBefore") LocalDateTime postedBefore,
            @Param("excludeAdIds") List<String> excludeAdIds,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            Pageable pageable
    );


    @Query(value = """
    SELECT 
        a.ad_id as adId,
        a.title as title,
        a.price as price,
        a.address as address,
        a.item_condition as itemCondition,
        a.is_negotiable as isNegotiable,
        a.is_featured as isFeatured,
        a.is_top_ad as isTopAd,
        a.view_count as viewCount,
        a.created_at as createdAt,
        a.expires_at as expiresAt,
        u.user_id as userUserId,
        u.first_name as userFirstName,
        u.last_name as userLastName,
        u.is_email_verified as userIsEmailVerified,
        cv.category_id as categoryVersionCategoryId,
        cv.name as categoryVersionName,
        cv.version_number as categoryVersionNumber,
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'firebaseUrl', m2.firebase_url,
                    'displayOrder', m2.display_order,
                    'mediaType', m2.media_type
                )
            )
            FROM (
                SELECT firebase_url, display_order, media_type
                FROM ad_media m2
                WHERE m2.ad_id = a.ad_id 
                AND m2.media_type = 'IMAGE'
                ORDER BY m2.display_order
            ) m2
        ) as mediaItems
    FROM advertisements a
    JOIN users u ON a.user_id = u.user_id
    JOIN category_versions cv ON a.category_version_id = cv.id
    WHERE a.ad_status = 'ACTIVE'
    AND (:searchTerm IS NULL OR 
        (a.title LIKE CONCAT('%', :searchTerm, '%') OR 
         a.description LIKE CONCAT('%', :searchTerm, '%')))
    AND (:categoryId IS NULL OR cv.category_id = :categoryId)
    AND (:categoryIds IS NULL OR cv.category_id IN (:categoryIds))
    AND (:minPrice IS NULL OR a.price >= :minPrice)
    AND (:maxPrice IS NULL OR a.price <= :maxPrice)
    AND (:condition IS NULL OR a.item_condition = :condition)
    AND (:verifiedSellersOnly IS NULL OR :verifiedSellersOnly = FALSE OR u.is_email_verified = TRUE)
    AND (:negotiable IS NULL OR :negotiable = FALSE OR a.is_negotiable = TRUE)
    AND (:hasPhotos IS NULL OR :hasPhotos = FALSE OR EXISTS (
        SELECT 1 FROM ad_media m 
        WHERE m.ad_id = a.ad_id AND m.media_type = 'IMAGE'
    ))
    AND (:featured IS NULL OR :featured = FALSE OR a.is_featured = TRUE)
    AND (:topAdsOnly IS NULL OR :topAdsOnly = FALSE OR a.is_top_ad = TRUE)
    AND (:sellerId IS NULL OR a.user_id = :sellerId)
    AND (:minViewCount IS NULL OR a.view_count >= :minViewCount)
    AND (:postedAfter IS NULL OR a.created_at >= :postedAfter)
    AND (:postedBefore IS NULL OR a.created_at <= :postedBefore)
    AND (:excludeAdIds IS NULL OR a.ad_id NOT IN (:excludeAdIds))
    ORDER BY
        CASE WHEN :sortBy = 'price' AND :sortDirection = 'ASC' THEN a.price END ASC,
        CASE WHEN :sortBy = 'price' AND :sortDirection = 'DESC' THEN a.price END DESC,
        CASE WHEN :sortBy = 'viewCount' AND :sortDirection = 'ASC' THEN a.view_count END ASC,
        CASE WHEN :sortBy = 'viewCount' AND :sortDirection = 'DESC' THEN a.view_count END DESC,
        CASE WHEN :sortBy = 'title' AND :sortDirection = 'ASC' THEN a.title END ASC,
        CASE WHEN :sortBy = 'title' AND :sortDirection = 'DESC' THEN a.title END DESC,
        CASE WHEN :sortBy = 'createdAt' OR :sortBy IS NULL THEN a.created_at END DESC
    """,
            countQuery = """
        SELECT COUNT(*)
        FROM advertisements a
        JOIN users u ON a.user_id = u.user_id
        JOIN category_versions cv ON a.category_version_id = cv.id
        WHERE a.ad_status = 'ACTIVE'
        AND (:searchTerm IS NULL OR 
            (a.title LIKE CONCAT('%', :searchTerm, '%') OR 
             a.description LIKE CONCAT('%', :searchTerm, '%')))
        AND (:categoryId IS NULL OR cv.category_id = :categoryId)
        AND (:categoryIds IS NULL OR cv.category_id IN (:categoryIds))
        AND (:minPrice IS NULL OR a.price >= :minPrice)
        AND (:maxPrice IS NULL OR a.price <= :maxPrice)
        AND (:condition IS NULL OR a.item_condition = :condition)
        AND (:verifiedSellersOnly IS NULL OR :verifiedSellersOnly = FALSE OR u.is_email_verified = TRUE)
        AND (:negotiable IS NULL OR :negotiable = FALSE OR a.is_negotiable = TRUE)
        AND (:hasPhotos IS NULL OR :hasPhotos = FALSE OR EXISTS (
            SELECT 1 FROM ad_media m 
            WHERE m.ad_id = a.ad_id AND m.media_type = 'IMAGE'
        ))
        AND (:featured IS NULL OR :featured = FALSE OR a.is_featured = TRUE)
        AND (:topAdsOnly IS NULL OR :topAdsOnly = FALSE OR a.is_top_ad = TRUE)
        AND (:sellerId IS NULL OR a.user_id = :sellerId)
        AND (:minViewCount IS NULL OR a.view_count >= :minViewCount)
        AND (:postedAfter IS NULL OR a.created_at >= :postedAfter)
        AND (:postedBefore IS NULL OR a.created_at <= :postedBefore)
        AND (:excludeAdIds IS NULL OR a.ad_id NOT IN (:excludeAdIds))
    """,
            nativeQuery = true)
    Page<AdvertisementListingProjection> findAdvertisementsForListing(
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") String condition,
            @Param("verifiedSellersOnly") Boolean verifiedSellersOnly,
            @Param("negotiable") Boolean negotiable,
            @Param("hasPhotos") Boolean hasPhotos,
            @Param("featured") Boolean featured,
            @Param("topAdsOnly") Boolean topAdsOnly,
            @Param("sellerId") Long sellerId,
            @Param("minViewCount") Integer minViewCount,
            @Param("postedAfter") LocalDateTime postedAfter,
            @Param("postedBefore") LocalDateTime postedBefore,
            @Param("excludeAdIds") List<String> excludeAdIds,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            Pageable pageable
    );

    Page<Advertisement> findByStatus(AdStatus status, Pageable pageable);
    Page<Advertisement> findByUserUserIdAndStatus(Long userId, AdStatus status, Pageable pageable);

    Page<Advertisement> findByUserUserIdAndStatusIn(Long userId, Collection<AdStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Advertisement a WHERE a.status = :status")
    long countByStatus(@Param("status") AdStatus status);

    @Query("SELECT COUNT(a) FROM Advertisement a WHERE a.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM Advertisement a WHERE DATE(a.createdAt) = DATE(:date)")
    long getTotalViewsForDate(@Param("date") LocalDateTime date);
}