package com.example.dsentrictestservice.repository;

import com.example.dsentrictestservice.core.EntityKind;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JsonStoreRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JsonStoreRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public long create(EntityKind kind, Map<String, Object> payload, Long orderUserId) {
        String sql = kind == EntityKind.ORDER
                ? "INSERT INTO orders (user_id, data) VALUES (?, CAST(? AS jsonb)) RETURNING id"
                : "INSERT INTO " + kind.tableName() + " (data) VALUES (CAST(? AS jsonb)) RETURNING id";
        String json = toJson(payload);
        Long id = kind == EntityKind.ORDER
                ? jdbcTemplate.queryForObject(sql, Long.class, orderUserId, json)
                : jdbcTemplate.queryForObject(sql, Long.class, json);
        return Objects.requireNonNull(id);
    }

    public Map<String, Object> read(EntityKind kind, long id) {
        String sql = "SELECT id, data::text FROM " + kind.tableName() + " WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> row(rs.getLong("id"), rs.getString("data")), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public void update(EntityKind kind, long id, Map<String, Object> payload, Long orderUserId) {
        String json = toJson(payload);
        if (kind == EntityKind.ORDER) {
            jdbcTemplate.update("UPDATE orders SET user_id = ?, data = CAST(? AS jsonb) WHERE id = ?", orderUserId, json, id);
        } else {
            jdbcTemplate.update("UPDATE " + kind.tableName() + " SET data = CAST(? AS jsonb) WHERE id = ?", json, id);
        }
    }

    public void delete(EntityKind kind, long id) {
        jdbcTemplate.update("DELETE FROM " + kind.tableName() + " WHERE id = ?", id);
    }

    public List<Map<String, Object>> list(EntityKind kind, int limit) {
        String sql = "SELECT id, data::text FROM " + kind.tableName() + " ORDER BY id LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> row(rs.getLong("id"), rs.getString("data")), limit);
    }

    public boolean exists(EntityKind kind, long id) {
        return !jdbcTemplate.query(
                "SELECT 1 FROM " + kind.tableName() + " WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getInt(1),
                id
        ).isEmpty();
    }

    public Set<Long> existingIds(EntityKind kind, List<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id FROM " + kind.tableName() + " WHERE id IN (" + placeholders + ")";
        return Set.copyOf(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), ids.toArray()));
    }

    public long count(EntityKind kind) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + kind.tableName(), Long.class);
        return count == null ? 0L : count;
    }

    public void truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE orders, products, users RESTART IDENTITY CASCADE");
    }

    private Map<String, Object> row(long id, String json) {
        Map<String, Object> data = fromJson(json);
        Map<String, Object> withId = new HashMap<>(data);
        withId.put("id", id);
        return withId;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize payload", e);
        }
    }
}
