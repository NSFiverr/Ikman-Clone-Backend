package com.marketplace.platform.dto.request;

import com.marketplace.platform.domain.admin.AdminType;
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
    private AdminType adminType;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String permissions;

    public boolean hasNoFilters() {
        return (email == null || email.isEmpty()) &&
                adminType == null &&
                createdAfter == null &&
                createdBefore == null &&
                (permissions == null || permissions.isEmpty());
    }
}
