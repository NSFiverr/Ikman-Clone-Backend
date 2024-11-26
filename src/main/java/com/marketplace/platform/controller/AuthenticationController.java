package com.marketplace.platform.controller;

import com.marketplace.platform.dto.request.AuthenticationRequest;
import com.marketplace.platform.dto.response.AuthenticationResponse.AdminAuthenticationResponse;
import com.marketplace.platform.dto.response.AuthenticationResponse.UserAuthenticationResponse;
import com.marketplace.platform.service.auth.AuthenticationService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentication endpoints for both admins and users")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final Bucket authenticationBucket;

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user or admin",
            description = "Authenticates either an admin or a user based on provided credentials"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "429", description = "Too many requests"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account deactivated or not verified")
    })
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        // Check rate limit
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

        // Authenticate and return appropriate response
        Object authResponse = authenticationService.authenticate(request);

        if (authResponse instanceof AdminAuthenticationResponse) {
            return ResponseEntity.ok()
                    .header("X-Rate-Limit-Remaining",
                            String.valueOf(probe.getRemainingTokens()))
                    .body((AdminAuthenticationResponse) authResponse);
        } else if (authResponse instanceof UserAuthenticationResponse) {
            return ResponseEntity.ok()
                    .header("X-Rate-Limit-Remaining",
                            String.valueOf(probe.getRemainingTokens()))
                    .body((UserAuthenticationResponse) authResponse);
        }

        // This should never happen if the service is implemented correctly
        return ResponseEntity.internalServerError().build();
    }
}