package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.AdPackage;
import com.marketplace.platform.domain.advertisement.VisibilityLevel;
import com.marketplace.platform.dto.request.AdPackageCreateRequest;
import com.marketplace.platform.dto.response.AdPackageResponse;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.exception.ResourceNotFoundException;
import com.marketplace.platform.mapper.AdPackageMapper;
import com.marketplace.platform.repository.advertisement.AdPackageRepository;
import com.marketplace.platform.validator.advertisement.AdPackageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = "adPackages")
public class AdPackageServiceImpl implements AdPackageService {
    private final AdPackageRepository adPackageRepository;
    private final AdPackageValidator validator;
    private final AdPackageMapper mapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "adPackages", allEntries = true),
            @CacheEvict(value = "activeAdPackages", allEntries = true)
    })
    public AdPackageResponse createPackage(AdPackageCreateRequest request) {
        log.debug("Creating new ad package with name: {}", request.getName());
        validator.validateCreateRequest(request);

        try {
            AdPackage adPackage = mapper.toEntity(request);
            adPackage = adPackageRepository.save(adPackage);
            log.info("Created new ad package with id: {}", adPackage.getPackageId());
            return mapper.toResponse(adPackage);
        } catch (Exception e) {
            log.error("Failed to create ad package", e);
            throw new BadRequestException("Failed to create ad package: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "adPackages", key = "#id"),
            @CacheEvict(value = "activeAdPackages", allEntries = true)
    })
    public AdPackageResponse updatePackage(Long id, AdPackageCreateRequest request) {
        log.debug("Updating ad package with id: {}", id);
        validator.validateUpdateRequest(id, request);

        AdPackage adPackage = adPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));

        try {
            mapper.updateEntity(adPackage, request);
            adPackage = adPackageRepository.save(adPackage);
            log.info("Updated ad package with id: {}", id);
            return mapper.toResponse(adPackage);
        } catch (Exception e) {
            log.error("Failed to update ad package with id: {}", id, e);
            throw new BadRequestException("Failed to update ad package: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "adPackages", key = "#id", unless = "#result == null")
    public AdPackageResponse getPackage(Long id) {
        log.debug("Fetching ad package with id: {}", id);
        return adPackageRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Ad package not found with id: {}", id);
                    return new ResourceNotFoundException("Package not found with id: " + id);
                });
    }

    @Override
    @Cacheable(value = "activeAdPackages", unless = "#result.isEmpty()")
    public List<AdPackageResponse> getAllActivePackages() {
        log.debug("Fetching all active ad packages");
        List<AdPackageResponse> packages = adPackageRepository.findAllActive().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        log.debug("Found {} active packages", packages.size());
        return packages;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "adPackages", key = "#id"),
            @CacheEvict(value = "activeAdPackages", allEntries = true)
    })
    public void deletePackage(Long id) {
        log.debug("Attempting to delete ad package with id: {}", id);
        validator.validateDeleteRequest(id);

        AdPackage adPackage = adPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));

        try {
            adPackage.setVisibilityLevel(VisibilityLevel.HIDDEN);
            adPackageRepository.save(adPackage);
            log.info("Successfully deleted (hidden) ad package with id: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete ad package with id: {}", id, e);
            throw new BadRequestException("Failed to delete ad package: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000) // Clear cache every hour
    @Caching(evict = {
            @CacheEvict(value = "adPackages", allEntries = true),
            @CacheEvict(value = "activeAdPackages", allEntries = true)
    })
    public void clearCache() {
        log.info("Clearing ad packages cache");
    }
}