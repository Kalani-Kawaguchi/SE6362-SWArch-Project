package com.se6362.kwic.controller;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController 
@RequestMapping ("/api")
public class HealthController {

    private final Optional<JdbcTemplate> jdbcTemplate;

    public HealthController(Optional<JdbcTemplate> jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public String health() {
        return "KWIC backend is running!";
    }

    @GetMapping("/db-health")
    public ResponseEntity<String> dbHealth() {
        if (jdbcTemplate.isEmpty()) {
            return ResponseEntity.status(503).body("Database is not configured in the local profile.");
        }
        Integer result = jdbcTemplate.get().queryForObject("SELECT 1", Integer.class);
        if (result != null && result == 1) {
            return ResponseEntity.ok("Database connection is healthy!");
        }

        return ResponseEntity.status(503).body("Database connection is not healthy!");
    }
}
