package com.example.dsentrictestservice.model;

public record MetricsStatsResponse(
        long count,
        double avgMs,
        long p50Ms,
        long p95Ms,
        long p99Ms,
        long minMs,
        long maxMs
) {
}
