package com.example.dsentrictestservice.model;

import java.util.Map;

public record LoadTestRequest(
        String endpoint,
        String method,
        int concurrency,
        int requests,
        Map<String, Object> body
) {
}
