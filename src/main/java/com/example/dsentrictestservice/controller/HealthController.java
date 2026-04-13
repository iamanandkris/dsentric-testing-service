package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.model.HealthResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return new HealthResponse("UP", result != null && result == 1 ? "connected" : "disconnected");
    }
}
