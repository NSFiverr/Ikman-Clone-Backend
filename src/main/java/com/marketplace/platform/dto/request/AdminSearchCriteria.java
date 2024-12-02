package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.admin.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSearchCriteria {
    private String email;
    private Role role;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String permissions;
}
