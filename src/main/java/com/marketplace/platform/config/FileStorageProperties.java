package com.marketplace.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    private String uploadDir = "uploads";
    private long maxFileSize = 5242880L; // 5MB default
    private String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif"};
}