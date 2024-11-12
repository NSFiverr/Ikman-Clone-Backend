package com.marketplace.platform.repository.category;

public interface CategoryStatsProjection {
    Long getCategoryId();
    String getCategoryName();
    Long getAdCount();
    Long getActiveAdCount();
}
