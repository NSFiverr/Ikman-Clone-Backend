package com.marketplace.platform.dto.response;

import com.marketplace.platform.domain.admin.AdminType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminResponse {
    private Long adminId;
    private String email;
    private AdminType adminType;
    private String permissions;
    private LocalDateTime lastAccessAt;
    private LocalDateTime createdAt;
}