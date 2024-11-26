package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
//import com.marketplace.platform.dto.request.AdvertisementSearchCriteria;
//import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdvertisementService {
    AdvertisementResponse createAdvertisement(AdvertisementCreateRequest request, User currentUser);
    AdvertisementResponse getAdvertisement(Long id);
    Page<AdvertisementResponse> getAllAdvertisements(Pageable pageable);
    public Page<AdvertisementResponse> getUserAdvertisements(Long userId, Pageable pageable);

    AdvertisementResponse updateAdvertisement(Long id, AdvertisementUpdateRequest request, User currentUser);

    public void deleteAdvertisement(Long id, User currentUser);

//    AdvertisementResponse getAdvertisement(Long id);
//
//    Page<AdvertisementResponse> searchAdvertisements(AdvertisementSearchCriteria criteria, Pageable pageable);
//
//    AdvertisementResponse updateAdvertisement(Long id, AdvertisementUpdateRequest request, User currentUser);
//
//    void deleteAdvertisement(Long id, User currentUser);
//
//    void updateStatus(Long id, AdStatus status, User currentUser);
//
//    void incrementViewCount(Long id);
//
//    Page<AdvertisementResponse> getUserAdvertisements(Long userId, Pageable pageable);
//
//    Page<AdvertisementResponse> getFavoriteAdvertisements(Long userId, Pageable pageable);
}