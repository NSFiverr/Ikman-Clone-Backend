package com.marketplace.platform.repository.advertisement;

import org.springframework.stereotype.Repository;

@Repository
public interface AdvertisementRepository {
    boolean existsByCategoryIdAndStatusActive(Long categoryId);
}