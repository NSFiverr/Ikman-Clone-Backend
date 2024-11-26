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
    private String secret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"; // Default secret (change in production)

    @NotBlank(message = "JWT issuer is required")
    private String issuer = "marketplace-platform"; // Default issuer

    @NotNull(message = "Access token validity is required")
    private Long accessTokenValidity = 3600000L; // 1 hour default

    @NotNull(message = "Refresh token validity is required")
    private Long refreshTokenValidity = 86400000L; // 24 hours default
}
//@ConfigurationProperties(prefix = "jwt")
//@Getter
//@Setter
//@Validated
//public class JwtProperties {
//    @NotBlank(message = "JWT secret key is required")
//    private String secret;
//
//    @NotBlank(message = "JWT issuer is required")
//    private String issuer;
//
//    @NotNull(message = "Access token validity is required")
//    private Long accessTokenValidity;
//
//    @NotNull(message = "Refresh token validity is required")
//    private Long refreshTokenValidity;
//}