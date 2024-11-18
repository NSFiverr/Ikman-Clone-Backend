package com.marketplace.platform.controller;

import com.marketplace.platform.domain.advertisement.MediaType;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.dto.response.AdvertisementResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.mapper.AdvertisementRequestDeserializer;
import com.marketplace.platform.service.advertisement.AdMediaService;
import com.marketplace.platform.service.advertisement.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal User currentUser
    ) {
        if (files.size() != mediaTypes.size() || files.size() != displayOrders.size()) {
            throw new BadRequestException("Number of files, media types, and display orders must match");
        }

        AdvertisementCreateRequest request = deserializer.deserialize(advertisementData);
        request.setMediaItems(adMediaService.createMediaRequests(files, mediaTypes, displayOrders));

        AdvertisementResponse response = advertisementService.createAdvertisement(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdvertisementResponse> getAdvertisement(@PathVariable Long id) {
        AdvertisementResponse response = advertisementService.getAdvertisement(id);
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

    @PutMapping("/{id}")
    public ResponseEntity<AdvertisementResponse> updateAdvertisement(
            @PathVariable Long id,
            @RequestBody AdvertisementUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        AdvertisementResponse response = advertisementService.updateAdvertisement(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvertisement(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        advertisementService.deleteAdvertisement(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
