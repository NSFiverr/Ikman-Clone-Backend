package com.marketplace.platform.domain.advertisement;

import lombok.Getter;

@Getter
public enum AdStatus {
    DRAFT,
    ACTIVE,
    PENDING_REVIEW,
    SUSPENDED,
    EXPIRED,
    DELETED
}

