package com.marketplace.platform.repository.advertisement;

import com.marketplace.platform.domain.advertisement.AdMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdMediaRepository extends JpaRepository<AdMedia, Long> {
    List<AdMedia> findByAdvertisementAdIdOrderByDisplayOrderAsc(Long adId);

    @Query("SELECT m FROM AdMedia m WHERE m.advertisement.adId = :adId")
    List<AdMedia> findActiveByAdvertisementId(@Param("adId") Long adId);

    void deleteByAdvertisementAdId(Long adId);
}
