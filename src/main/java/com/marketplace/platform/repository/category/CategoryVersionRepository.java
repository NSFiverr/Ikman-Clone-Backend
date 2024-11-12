package com.marketplace.platform.repository.category;

import com.marketplace.platform.domain.category.CategoryStatus;
import com.marketplace.platform.domain.category.CategoryVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validFrom <= :timestamp " +
            "AND (cv.validTo IS NULL OR cv.validTo > :timestamp)")
    Optional<CategoryVersion> findVersionAtTime(
            @Param("categoryId") Long categoryId,
            @Param("timestamp") LocalDateTime timestamp
    );


    List<CategoryVersion> findByCategoryIdOrderByVersionNumberDesc(Long categoryId);

    @Query("SELECT MAX(cv.versionNumber) FROM CategoryVersion cv WHERE cv.category.categoryId = :categoryId")
    Optional<Integer> findLatestVersionNumber(@Param("categoryId") Long categoryId);


    Page<CategoryVersion> findByCategoryIdAndStatus(
            Long categoryId,
            CategoryStatus status,
            Pageable pageable
    );


    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validTo > CURRENT_TIMESTAMP")
    List<CategoryVersion> findPendingVersions(@Param("categoryId") Long categoryId);

    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validFrom < :endTime " +
            "AND (cv.validTo IS NULL OR cv.validTo > :startTime) " +
            "AND cv.id <> :excludeVersionId")
    List<CategoryVersion> findOverlappingVersions(
            @Param("categoryId") Long categoryId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeVersionId") Long excludeVersionId
    );


    @Query("SELECT DISTINCT cv FROM CategoryVersion cv " +
            "JOIN cv.advertisements a " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND a.status = 'ACTIVE'")
    List<CategoryVersion> findVersionsWithActiveAds(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(a) FROM Advertisement a " +
            "WHERE a.categoryVersion.id = :versionId")
    long countAdvertisementsUsingVersion(@Param("versionId") Long versionId);


    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validFrom > CURRENT_TIMESTAMP")
    List<CategoryVersion> findFutureVersions(@Param("categoryId") Long categoryId);


    @Query("SELECT cv FROM CategoryVersion cv " +
            "WHERE cv.category.categoryId = :categoryId " +
            "AND cv.validFrom <= :endDate " +
            "AND (cv.validTo IS NULL OR cv.validTo >= :startDate)")
    List<CategoryVersion> findVersionsActiveBetween(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("SELECT COUNT(c) > 0 FROM Category c " +
            "WHERE c.parent.categoryId = :categoryId")
    boolean hasChildCategories(@Param("categoryId") Long categoryId);


    Optional<CategoryVersion> findByCategoryIdAndVersionNumber(
            Long categoryId,
            Integer versionNumber
    );
}