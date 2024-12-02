package com.marketplace.platform.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalAdmins;
    private long totalPackages;
    private long totalCategories;
    private long totalAdvertisements;
    private long activeAdvertisements;
    private DailyStats todayStats;

    @Data
    @Builder
    public static class DailyStats {
        private long newUsers;
        private long newAdvertisements;
        private long totalViews;
    }
}
