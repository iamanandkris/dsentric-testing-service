package com.example.dsentrictestservice.service;

import com.example.dsentrictestservice.model.MetricsStatsResponse;
import com.example.dsentrictestservice.repository.MetricsRepository;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final MetricsRepository metricsRepository;

    public MetricsService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    public MetricsStatsResponse stats(String endpoint, String language) {
        return metricsRepository.stats(endpoint, language);
    }
}
