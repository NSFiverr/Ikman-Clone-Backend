package com.marketplace.platform.controller;

import com.marketplace.platform.domain.advertisement.MediaType;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.projection.AdvertisementListingProjection;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
import com.marketplace.platform.dto.request.AdvertisementSearchCriteria;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.mapper.AdvertisementRequestDeserializer;
import com.marketplace.platform.service.advertisement.AdMediaService;
import com.marketplace.platform.service.advertisement.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;
    private final AdMediaService adMediaService;
    private final AdvertisementRequestDeserializer deserializer;

    // Create Advertisement
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdvertisementResponse> createAdvertisement(
            @RequestParam("advertisementData") String advertisementData,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("mediaTypes") List<MediaType> mediaTypes,
            @RequestParam("displayOrders") List<Integer> displayOrders,
            @RequestParam(value = "paymentProof", required = false) MultipartFile paymentProof,
            @RequestHeader("Authorization") String accessToken
    ) {
        if (files.size() != mediaTypes.size() || files.size() != displayOrders.size()) {
            throw new BadRequestException("Number of files, media types, and display orders must match");
        }

        AdvertisementCreateRequest request = deserializer.deserialize(advertisementData);
        request.setMediaItems(adMediaService.createMediaRequests(files, mediaTypes, displayOrders));
        request.setPaymentProof(paymentProof);

        AdvertisementResponse response = advertisementService.createAdvertisement(request, accessToken);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdvertisementResponse> getAdvertisement(
            @PathVariable Long id,
            @RequestHeader("Authorization") String accessToken
    ) {
        AdvertisementResponse response = advertisementService.getAdvertisement(id, accessToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AdvertisementResponse>> getAllAdvertisements(Pageable pageable) {
        Page<AdvertisementResponse> response = advertisementService.getAllAdvertisements(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AdvertisementResponse>> getUserAdvertisements(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        Page<AdvertisementResponse> response = advertisementService.getUserAdvertisements(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdvertisementResponse> updateAdvertisement(
            @PathVariable Long id,
            @RequestParam("advertisementData") String advertisementData,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "mediaTypes", required = false) List<MediaType> mediaTypes,
            @RequestParam(value = "displayOrders", required = false) List<Integer> displayOrders,
            @RequestParam(value = "retainedMediaIds", required = false) List<Long> retainedMediaIds,
            @RequestHeader("Authorization") String accessToken
    ) {
        // Validate media params consistency
        if (files != null) {
            if (mediaTypes == null || displayOrders == null ||
                    files.size() != mediaTypes.size() || files.size() != displayOrders.size()) {
                throw new BadRequestException("When providing new files, media types and display orders must match");
            }
        }

        // Deserialize the advertisement data
        AdvertisementUpdateRequest request = deserializer.deserializeUpdate(advertisementData);

        AdvertisementResponse response = advertisementService.updateAdvertisement(
                id,
                request,
                files,
                mediaTypes,
                displayOrders,
                retainedMediaIds,
                accessToken
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvertisement(
            @PathVariable Long id,
            @RequestHeader("Authorization") String accessToken
    ) {
        advertisementService.deleteAdvertisement(id, accessToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<Page<AdvertisementListingProjection>> getNearbyAdvertisements(
            @ParameterObject AdvertisementSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(advertisementService.getNearbyAdvertisements(criteria, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AdvertisementListingProjection>> searchAdvertisements(
            @ParameterObject @Valid AdvertisementSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(advertisementService.searchAdvertisements(criteria, pageable));
    }

    @GetMapping("/moderation/pending")
    public ResponseEntity<Page<AdvertisementResponse>> getPendingReviewAdvertisements(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(advertisementService.getPendingReviewAdvertisements(pageable));
    }

    @GetMapping("/moderation/pending/{id}")
    public ResponseEntity<AdvertisementResponse> getPendingAdvertisement(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(advertisementService.getPendingAdvertisement(id));
    }

    @PostMapping("/{id}/moderation/approve")
    public ResponseEntity<AdvertisementResponse> approveAdvertisement(
            @PathVariable Long id,
            @RequestHeader("Authorization") String accessToken
    ) {
        return ResponseEntity.ok(advertisementService.approveAdvertisement(id, accessToken));
    }

    @PostMapping("/{id}/moderation/reject")
    public ResponseEntity<AdvertisementResponse> rejectAdvertisement(
            @PathVariable Long id,
            @RequestHeader("Authorization") String accessToken
    ) {
        return ResponseEntity.ok(advertisementService.rejectAdvertisement(id, accessToken));
    }

}
