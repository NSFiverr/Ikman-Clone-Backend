package com.marketplace.platform.dto.request;

import lombok.Data;

@Data
public class UserSearchCriteria {
    private String searchTerm;
    private String status;
    private String startDate;
    private String endDate;

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }
}
