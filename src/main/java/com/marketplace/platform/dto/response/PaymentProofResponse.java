package com.marketplace.platform.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentProofResponse {
    private Long id;
    private String firebaseUrl;
    private String originalFilename;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}

