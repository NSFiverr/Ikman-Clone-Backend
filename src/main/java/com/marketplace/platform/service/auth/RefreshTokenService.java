package com.marketplace.platform.service.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.exception.InvalidTokenException;
import com.marketplace.platform.exception.TokenExpiredException;
import com.marketplace.platform.repository.token.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final HttpServletRequest request;

    @Transactional
    public RefreshToken createRefreshTokenForAdmin(Admin admin) {
        RefreshToken refreshToken = RefreshToken.builder()
                .admin(admin)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7)) // 7 days validity
                .deviceInfo(extractDeviceInfo())
                .ipAddress(extractIpAddress())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken createRefreshTokenForUser(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .deviceInfo(extractDeviceInfo())
                .ipAddress(extractIpAddress())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            throw new TokenExpiredException("Refresh token expired");
        }

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token, String revokedBy, String reason) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshToken.setRevokedBy(revokedBy);
        refreshToken.setRevocationReason(reason);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(User user, String revokedBy, String reason) {
        refreshTokenRepository.revokeAllUserTokens(
                user,
                LocalDateTime.now(),
                revokedBy,
                reason
        );
    }

    @Transactional
    public void revokeAllAdminTokens(Admin admin, String revokedBy, String reason) {
        refreshTokenRepository.revokeAllAdminTokens(
                admin,
                LocalDateTime.now(),
                revokedBy,
                reason
        );
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }

    private String extractDeviceInfo() {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    private String extractIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}