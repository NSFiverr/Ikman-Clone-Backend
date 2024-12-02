package com.marketplace.platform.service.auth;

import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import com.marketplace.platform.exception.InvalidTokenException;
import com.marketplace.platform.exception.TokenExpiredException;
import com.marketplace.platform.mapper.AuthenticationMapper;
import com.marketplace.platform.repository.token.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationMapper authenticationMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Value("${app.jwt.cookie-name:refreshToken}")
    private String cookieName;

    @Override
    @Transactional
    public TokenRefreshResponse refreshAccessToken() {
        try {
            String refreshTokenValue = extractRefreshTokenFromCookies();
            RefreshToken refreshToken = verifyToken(refreshTokenValue);
            boolean remember = refreshToken.getExpiryDate()
                    .isAfter(LocalDateTime.now().plusDays(jwtProperties.getShortRefreshTokenValidityInDays()));

            TokenRefreshResponse response;
            String email;

            if (refreshToken.getAdmin() != null) {
                Admin admin = refreshToken.getAdmin();
                String newAccessToken = jwtService.generateAdminAccessToken(admin);
                email = admin.getEmail();
                createRefreshTokenForAdmin(admin, remember);
                response = authenticationMapper.toRefreshResponse(admin, newAccessToken);
            } else if (refreshToken.getUser() != null) {
                User user = refreshToken.getUser();
                String newAccessToken = jwtService.generateUserAccessToken(user);
                email = user.getEmail();
                createRefreshTokenForUser(user, remember);
                response = authenticationMapper.toRefreshResponse(user, newAccessToken);
            } else {
                clearRefreshTokenCookie();
                throw new InvalidTokenException("Invalid token state");
            }

            // Revoke the old refresh token
            revokeToken(refreshTokenValue, email, "Token refresh");

            return response;

        } catch (TokenExpiredException | InvalidTokenException e) {
            // Clear the cookie before propagating the error
            clearRefreshTokenCookie();
            throw e;
        }
    }

    @Override
    @Transactional
    public RefreshToken createRefreshTokenForAdmin(Admin admin, boolean remember) {
        RefreshToken refreshToken = RefreshToken.builder()
                .admin(admin)
                .token(jwtService.generateRefreshToken())
                .expiryDate(LocalDateTime.now().plusDays(
                        jwtProperties.getRefreshTokenValidityInDays(remember)
                ))
                .deviceInfo(extractDeviceInfo())
                .ipAddress(extractIpAddress())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        setRefreshTokenCookie(savedToken.getToken(), remember);
        return savedToken;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshTokenForUser(User user, boolean remember) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtService.generateRefreshToken())
                .expiryDate(LocalDateTime.now().plusDays(
                        jwtProperties.getRefreshTokenValidityInDays(remember)
                ))
                .deviceInfo(extractDeviceInfo())
                .ipAddress(extractIpAddress())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        setRefreshTokenCookie(savedToken.getToken(), remember);
        return savedToken;
    }

    @Override
    public void setRefreshTokenCookie(String token, boolean remember) {
        int validityDays = jwtProperties.getRefreshTokenValidityInDays(remember);

        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // for HTTPS
        cookie.setPath("/");    // cookie path
        cookie.setMaxAge(validityDays * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    @Override
    public void clearRefreshTokenCookie() {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Override
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

    @Override
    @Transactional
    public void revokeToken(String token, String revokedBy, String reason) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshToken.setRevokedBy(revokedBy);
        refreshToken.setRevocationReason(reason);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user, String revokedBy, String reason) {
        refreshTokenRepository.revokeAllUserTokens(
                user,
                LocalDateTime.now(),
                revokedBy,
                reason
        );
    }

    @Override
    @Transactional
    public void revokeAllAdminTokens(Admin admin, String revokedBy, String reason) {
        refreshTokenRepository.revokeAllAdminTokens(
                admin,
                LocalDateTime.now(),
                revokedBy,
                reason
        );
    }

    @Override
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

    private String extractRefreshTokenFromCookies() {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        throw new InvalidTokenException("Refresh token not found in cookies");
    }

}