package com.example.dsentrictestservice.service;

import com.example.dsentrictestservice.repository.MetricsRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MetricsLogger {
    private final MetricsRepository metricsRepository;

    public MetricsLogger(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Async
    public void log(String endpoint, String language, long executionTimeMs, boolean success) {
        metricsRepository.insert(endpoint, language, executionTimeMs, success);
    }
}
