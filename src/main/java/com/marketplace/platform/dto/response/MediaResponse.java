package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaResponse {
    private Long mediaId;
    private String mediaType;
    private String firebaseUrl;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private Integer displayOrder;
}

