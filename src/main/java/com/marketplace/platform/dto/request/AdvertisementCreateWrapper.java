package com.marketplace.platform.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AdvertisementCreateWrapper {
    private String advertisementData;
    private List<MultipartFile> files;
}