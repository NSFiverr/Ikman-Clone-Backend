package com.marketplace.platform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok("Server is running!");
    }

    @GetMapping("/database")
    public ResponseEntity<?> databaseHealthCheck() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok("Database connection successful!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Database connection failed: " + e.getMessage());
        }
    }
}
