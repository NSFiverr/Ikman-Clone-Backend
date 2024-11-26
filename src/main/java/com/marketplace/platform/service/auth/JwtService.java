package com.marketplace.platform.service.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAdminAccessToken(Admin admin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getAdminId());
        claims.put("adminType", admin.getAdminType().toString());
        claims.put("permissions", admin.getPermissions());
        claims.put("tokenType", "ACCESS");
        claims.put("role", "ADMIN");

        return createToken(claims, admin.getEmail(), jwtProperties.getAccessTokenValidity());
    }

    public String generateAdminRefreshToken(Admin admin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getAdminId());
        claims.put("tokenType", "REFRESH");
        claims.put("role", "ADMIN");

        return createToken(claims, admin.getEmail(), jwtProperties.getRefreshTokenValidity());
    }

    public String generateUserAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("status", user.getStatus().toString());
        claims.put("tokenType", "ACCESS");
        claims.put("role", "USER");

        return createToken(claims, user.getEmail(), jwtProperties.getAccessTokenValidity());
    }

    public String generateUserRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("tokenType", "REFRESH");
        claims.put("role", "USER");

        return createToken(claims, user.getEmail(), jwtProperties.getRefreshTokenValidity());
    }

    private String createToken(Map<String, Object> claims, String subject, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }
}