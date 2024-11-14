package com.marketplace.platform.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ValidationRules {
    // Text validation
    private Integer minLength;
    private Integer maxLength;

    // Number validation
    private Double minValue;
    private Double maxValue;
    private Integer decimals;

    // Select/Multi-select validation
    private List<String> options;

    // Date validation
    private String minDate;
    private String maxDate;

    // Common
    private String pattern;
    private String errorMessage;
}