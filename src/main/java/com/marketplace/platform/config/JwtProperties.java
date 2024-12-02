package com.marketplace.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Validated
public class JwtProperties {
    @NotBlank(message = "JWT secret key is required")
    private String secret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    @NotBlank(message = "JWT issuer is required")
    private String issuer = "marketplace-platform";

    @NotNull(message = "Access token validity is required")
    private Long accessTokenValidity = 30L * 60L * 1000L; // 30 minutes in milliseconds

    // Short-lived refresh token (1 day) - when "remember me" is false
    @NotNull(message = "Short refresh token validity is required")
    private Long shortRefreshTokenValidity = 1L * 24L * 60L * 60L * 1000L; // 1 day in milliseconds

    // Long-lived refresh token (30 days) - when "remember me" is true
    @NotNull(message = "Long refresh token validity is required")
    private Long longRefreshTokenValidity = 30L * 24L * 60L * 60L * 1000L; // 30 days in milliseconds

    public Long getAccessTokenValidityInMillis() {
        return accessTokenValidity;
    }

    public Long getShortRefreshTokenValidityInMillis() {
        return shortRefreshTokenValidity;
    }

    public Long getLongRefreshTokenValidityInMillis() {
        return longRefreshTokenValidity;
    }

    public int getShortRefreshTokenValidityInDays() {
        return (int)(shortRefreshTokenValidity / (24L * 60L * 60L * 1000L));
    }

    public int getLongRefreshTokenValidityInDays() {
        return (int)(longRefreshTokenValidity / (24L * 60L * 60L * 1000L));
    }

    public int getRefreshTokenValidityInDays(boolean remember) {
        return remember ? getLongRefreshTokenValidityInDays() : getShortRefreshTokenValidityInDays();
    }
}