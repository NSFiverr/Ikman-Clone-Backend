package com.marketplace.platform.service.auth;

import com.marketplace.platform.domain.admin.Admin;
import com.marketplace.platform.domain.token.RefreshToken;
import com.marketplace.platform.domain.user.User;
import com.marketplace.platform.config.JwtProperties;
import com.marketplace.platform.domain.user.UserStatus;
import com.marketplace.platform.dto.response.TokenRefreshResponse;
import com.marketplace.platform.exception.InvalidTokenException;
import com.marketplace.platform.repository.admin.AdminRepository;
import com.marketplace.platform.repository.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService{
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    private final AdminRepository adminRepository;

    private final UserRepository userRepository;


    public JwtServiceImpl(JwtProperties jwtProperties, AdminRepository adminRepository,  UserRepository userRepository) {
        this.jwtProperties = jwtProperties;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenRefreshResponse refreshTokenForEntity(RefreshToken refreshToken) {
        if (refreshToken.getAdmin() != null) {
            Admin admin = refreshToken.getAdmin();
            String newAccessToken = generateAdminAccessToken(admin);
            return TokenRefreshResponse.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .email(admin.getEmail())
                    .type("ADMIN")
                    .build();
        } else if (refreshToken.getUser() != null) {
            User user = refreshToken.getUser();
            String newAccessToken = generateUserAccessToken(user);
            return TokenRefreshResponse.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .email(user.getEmail())
                    .type("USER")
                    .build();
        }

        throw new InvalidTokenException("Invalid token state");
    }

    @Override
    public String generateAdminAccessToken(Admin admin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getAdminId());
        claims.put("adminType", admin.getRole().toString());
        claims.put("permissions", admin.getPermissions());
        claims.put("tokenType", "ACCESS");
        claims.put("role", "ADMIN");

        return createToken(claims, admin.getEmail(), jwtProperties.getAccessTokenValidity());
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String generateUserAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("status", user.getStatus().toString());
        claims.put("tokenType", "ACCESS");
        claims.put("role", "USER");

        return createToken(claims, user.getEmail(), jwtProperties.getAccessTokenValidity());
    }

    @Override
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

    @Override
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public String getEmailFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    @Override
    public Optional<Admin> getAdminFromToken(String token) {
        String jwt = token.substring(7);
        String adminEmail = getEmailFromToken(jwt);
        return adminRepository.findByEmail(adminEmail);
    }

    @Override
    public Optional<User> getUserFromToken(String token) {
        String jwt = token.substring(7);
        String userEmail = getEmailFromToken(jwt);
        return userRepository.findByEmail(userEmail, UserStatus.DELETED);
    }


    @Override
    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
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

}