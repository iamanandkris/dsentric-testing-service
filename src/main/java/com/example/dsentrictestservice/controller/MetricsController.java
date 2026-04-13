package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.model.MetricsStatsResponse;
import com.example.dsentrictestservice.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/stats")
    public MetricsStatsResponse stats(
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String language
    ) {
        return metricsService.stats(endpoint, language);
    }
}
