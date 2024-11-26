package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.CategoryStatus;
import com.marketplace.platform.domain.category.CategoryVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryVersionRepository extends JpaRepository<CategoryVersion, Long>, JpaSpecificationExecutor<CategoryVersion> {

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validTo IS NULL")
    Optional<CategoryVersion> findCurrentVersion(@Param("categoryId") Long categoryId);

    Optional<CategoryVersion> findTopByCategoryCategoryIdOrderByVersionNumberDesc(Long categoryId);


    @Query("SELECT cv FROM CategoryVersion cv " +
            "LEFT JOIN FETCH cv.attributes attr " +
            "LEFT JOIN FETCH attr.attributeDefinition " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.versionNumber = (" +
            "    SELECT MAX(v.versionNumber) " +
            "    FROM CategoryVersion v " +
            "    WHERE v.category.categoryId = :categoryId" +
            ")")
    Optional<CategoryVersion> findTopByCategoryCategoryIdWithAttributesOrderByVersionNumberDesc(
            @Param("categoryId") Long categoryId);


    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validFrom <= :timestamp " +
            "AND (cv.validTo IS NULL OR cv.validTo > :timestamp)")
    Optional<CategoryVersion> findVersionAtTime(
            @Param("categoryId") Long categoryId,
            @Param("timestamp") LocalDateTime timestamp
    );


    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "ORDER BY cv.versionNumber DESC")
    List<CategoryVersion> findVersionsForCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validTo IS NOT NULL " +
            "ORDER BY cv.validTo DESC")
    Page<CategoryVersion> findLastActiveVersion(@Param("categoryId") Long categoryId, Pageable pageable);



    @Query("SELECT cv FROM CategoryVersion cv " +
            "LEFT JOIN FETCH cv.attributes attr " +
            "LEFT JOIN FETCH attr.attributeDefinition " +
            "WHERE cv.category.categoryId = :categoryId " +
            "ORDER BY cv.versionNumber DESC")
    Page<CategoryVersion> findVersionHistoryByCategory(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );


    @Query("SELECT DISTINCT cv FROM CategoryVersion cv " +
            "JOIN cv.advertisements a " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND a.status = 'ACTIVE'")
    List<CategoryVersion> findVersionsWithActiveAds(@Param("categoryId") Long categoryId);


    @Query("SELECT COUNT(c) > 0 FROM Category c " +
            "WHERE c.parent.categoryId = :categoryId")
    boolean hasChildCategories(@Param("categoryId") Long categoryId);


    @Query("SELECT CASE WHEN COUNT(cv) > 0 THEN true ELSE false END FROM CategoryVersion cv " +
            "WHERE LOWER(cv.name) = LOWER(:name) " +
            "AND cv.validTo IS NULL " +
            "AND cv.category.status <> :excludeStatus")
    boolean existsByNameIgnoreCaseAndStatusNot(
            @Param("name") String name,
            @Param("excludeStatus") CategoryStatus excludeStatus
    );

    @Query("SELECT CASE WHEN COUNT(cv) > 0 THEN true ELSE false END FROM CategoryVersion cv " +
            "WHERE LOWER(cv.name) = LOWER(:name) " +
            "AND cv.validTo IS NULL " +
            "AND cv.category.status = :status")
    boolean existsByNameIgnoreCaseAndStatus(
            @Param("name") String name,
            @Param("status") CategoryStatus status
    );

    @Query("SELECT CASE WHEN COUNT(cv) > 0 THEN true ELSE false END FROM CategoryVersion cv " +
            "WHERE LOWER(cv.name) = LOWER(:name) " +
            "AND cv.category.categoryId <> :categoryId " +
            "AND cv.validTo IS NULL " +
            "AND cv.category.status = :status")
    boolean existsByNameIgnoreCaseAndCategoryIdNotAndStatus(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("status") CategoryStatus status
    );

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validTo IS NOT NULL " +  // Get closed versions
            "AND cv.validTo < (SELECT c.updatedAt FROM Category c WHERE c.categoryId = :categoryId) " +
            "ORDER BY cv.versionNumber DESC")
    List<CategoryVersion> findVersionsBeforeDeletion(@Param("categoryId") Long categoryId);



    @Query("SELECT MAX(cv.versionNumber) FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId")
    Optional<Integer> findMaxVersionNumberForCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validTo IS NOT NULL " +
            "ORDER BY cv.versionNumber DESC")
    List<CategoryVersion> findPreviousVersions(@Param("categoryId") Long categoryId);


    default Optional<CategoryVersion> findLastActiveVersion(Long categoryId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "validTo"));
        Page<CategoryVersion> result = findLastActiveVersion(categoryId, pageable);
        return result.getContent().stream().findFirst();
    }

}