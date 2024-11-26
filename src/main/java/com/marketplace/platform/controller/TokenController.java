package com.marketplace.platform.controller;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.request.TokenRefreshRequest;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import com.marketplace.platform.exception.InvalidTokenException;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.user.UserRepository;
import com.marketplace.platform.service.auth.JwtService;
import com.marketplace.platform.service.auth.RefreshTokenService;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenController {
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final Bucket tokenBucket;

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        if (!tokenBucket.tryConsume(1)) {
            return ResponseEntity.status(429).build(); // Too Many Requests
        }

        RefreshToken refreshToken = refreshTokenService.verifyToken(request.getRefreshToken());

        String newAccessToken;
        String email;
        String type;

        if (refreshToken.getAdmin() != null) {
            Admin admin = refreshToken.getAdmin();
            newAccessToken = jwtService.generateAdminAccessToken(admin);
            email = admin.getEmail();
            type = "ADMIN";
        } else if (refreshToken.getUser() != null) {
            User user = refreshToken.getUser();
            newAccessToken = jwtService.generateUserAccessToken(user);
            email = user.getEmail();
            type = "USER";
        } else {
            throw new InvalidTokenException("Invalid token state");
        }

        return ResponseEntity.ok(TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .email(email)
                .type(type)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest request) {
        refreshTokenService.revokeToken(
                request.getRefreshToken(),
                jwtService.getEmailFromToken(request.getAccessToken()),
                "User logout"
        );
        return ResponseEntity.ok().build();
    }
}