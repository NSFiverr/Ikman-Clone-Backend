package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.advertisement.AdStatus;
import com.marketplace.platform.domain.advertisement.MediaType;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.projection.AdvertisementListingProjection;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
//import com.marketplace.platform.dto.request.AdvertisementSearchCriteria;
//import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.request.AdvertisementSearchCriteria;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdvertisementService {
    AdvertisementResponse createAdvertisement(AdvertisementCreateRequest request, String  token);

    AdvertisementResponse approveAdvertisement(Long adId, String token);
    AdvertisementResponse rejectAdvertisement(Long adId, String token);
    Page<AdvertisementResponse> getPendingReviewAdvertisements(Pageable pageable);
    AdvertisementResponse getAdvertisement(Long id, String token);
    Page<AdvertisementResponse> getAllAdvertisements(Pageable pageable);
    public Page<AdvertisementResponse> getUserAdvertisements(Long userId, Pageable pageable);

    AdvertisementResponse updateAdvertisement(
            Long id,
            AdvertisementUpdateRequest request,
            List<MultipartFile> newFiles,
            List<MediaType> newMediaTypes,
            List<Integer> newDisplayOrders,
            List<Long> retainedMediaIds,
            String token
    );

    public void deleteAdvertisement(Long id, String accessToken);

    Page<AdvertisementListingProjection> searchAdvertisements(AdvertisementSearchCriteria criteria, Pageable pageable);
    Page<AdvertisementListingProjection> getNearbyAdvertisements(AdvertisementSearchCriteria criteria, Pageable pageable);

    AdvertisementResponse getPendingAdvertisement(Long id);

}