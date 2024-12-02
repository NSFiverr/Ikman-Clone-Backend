package com.marketplace.platform.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.platform.dto.request.AdvertisementCreateRequest;
import com.marketplace.platform.dto.request.AdvertisementUpdateRequest;
import com.marketplace.platform.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdvertisementRequestDeserializer {
    private final ObjectMapper objectMapper;

    public AdvertisementRequestDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AdvertisementCreateRequest deserialize(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, AdvertisementCreateRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize advertisement data", e);
            throw new BadRequestException("Invalid advertisement data format");
        }
    }

    public AdvertisementUpdateRequest deserializeUpdate(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, AdvertisementUpdateRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize advertisement update data", e);
            throw new BadRequestException("Invalid advertisement update data format");
        }
    }
}
