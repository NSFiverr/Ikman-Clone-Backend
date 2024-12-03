package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import com.marketplace.platform.service.auth.AuthenticationService;
import com.marketplace.platform.service.auth.RefreshTokenService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final Bucket authenticationBucket;
    private final Bucket tokenBucket;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        ConsumptionProbe probe = authenticationBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            return ResponseEntity
                    .status(429)
                    .header("X-Rate-Limit-Retry-After-Milliseconds",
                            String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000))
                    .header("X-Rate-Limit-Remaining",
                            String.valueOf(probe.getRemainingTokens()))
                    .build();
        }

        Object authResponse = authenticationService.authenticate(request);
        return ResponseEntity.ok()
                .header("X-Rate-Limit-Remaining",
                        String.valueOf(probe.getRemainingTokens()))
                .body(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken() {
        if (!tokenBucket.tryConsume(1)) {
            return ResponseEntity.status(429).build();
        }

        TokenRefreshResponse response = refreshTokenService.refreshAccessToken();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authenticationService.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> initiatePasswordReset(@RequestParam String email) {
        authenticationService.initiatePasswordReset(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        authenticationService.resetPassword(token, newPassword);
        return ResponseEntity.noContent().build();
    }
}