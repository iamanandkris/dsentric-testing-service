package com.example.dsentrictestservice.repository;

import com.example.dsentrictestservice.model.MetricsStatsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class MetricsRepository {
    private final JdbcTemplate jdbcTemplate;

    public MetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String endpoint, String language, long executionTimeMs, boolean success) {
        jdbcTemplate.update(
                "INSERT INTO metrics (endpoint, language, execution_time_ms, success) VALUES (?, ?, ?, ?)",
                endpoint,
                language,
                executionTimeMs,
                success
        );
    }

    public MetricsStatsResponse stats(String endpoint, String language) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (endpoint != null && !endpoint.isBlank()) {
            where.append(" AND endpoint = ? ");
            args.add(endpoint);
        }
        if (language != null && !language.isBlank()) {
            where.append(" AND language = ? ");
            args.add(language);
        }

        String sql = """
                SELECT
                    COUNT(*) AS count,
                    COALESCE(AVG(execution_time_ms), 0) AS avg_ms,
                    COALESCE(percentile_cont(0.50) WITHIN GROUP (ORDER BY execution_time_ms), 0) AS p50_ms,
                    COALESCE(percentile_cont(0.95) WITHIN GROUP (ORDER BY execution_time_ms), 0) AS p95_ms,
                    COALESCE(percentile_cont(0.99) WITHIN GROUP (ORDER BY execution_time_ms), 0) AS p99_ms,
                    COALESCE(MIN(execution_time_ms), 0) AS min_ms,
                    COALESCE(MAX(execution_time_ms), 0) AS max_ms
                FROM metrics
                """ + where;

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, args.toArray());
        return new MetricsStatsResponse(
                ((Number) row.get("count")).longValue(),
                ((Number) row.get("avg_ms")).doubleValue(),
                Math.round(((Number) row.get("p50_ms")).doubleValue()),
                Math.round(((Number) row.get("p95_ms")).doubleValue()),
                Math.round(((Number) row.get("p99_ms")).doubleValue()),
                ((Number) row.get("min_ms")).longValue(),
                ((Number) row.get("max_ms")).longValue()
        );
    }
}
