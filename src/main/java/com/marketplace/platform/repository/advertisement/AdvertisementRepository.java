package com.marketplace.platform.repository.advertisement;

import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Advertisement a " +
            "WHERE a.categoryVersion.category.categoryId = :categoryId " +
            "AND a.status = 'ACTIVE'")
    boolean existsByCategoryIdAndStatusActive(@Param("categoryId") Long categoryId);


    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Advertisement a " +
            "WHERE a.categoryVersion.id = :categoryVersionId " +
            "AND a.status = 'ACTIVE'")
    boolean existsByCategoryVersionIdAndStatusActive(@Param("categoryVersionId") Long categoryVersionId);

    Page<Advertisement> findByStatus(AdStatus status, Pageable pageable);
    Page<Advertisement> findByUserUserIdAndStatus(Long userId, AdStatus status, Pageable pageable);
}