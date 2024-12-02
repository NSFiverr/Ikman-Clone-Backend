package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminAuditLogResponse {
    private Long id;
    private String action;
    private Long adminId;
    private Long performedBy;
    private String reason;
    private AdminResponse adminDetails;
    private LocalDateTime createdAt;
}