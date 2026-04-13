package com.example.dsentrictestservice.model;

public record LoadTestResponse(
        int totalRequests,
        int successfulRequests,
        int failedRequests,
        long totalDurationMs,
        double avgLatencyMs,
        double requestsPerSecond
) {
}
