package com.marketplace.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // The @EnableRetry annotation is sufficient for basic retry functionality
    // Additional customization can be added here if needed
}