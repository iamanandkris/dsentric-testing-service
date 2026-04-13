package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.model.LoadTestRequest;
import com.example.dsentrictestservice.model.LoadTestResponse;
import com.example.dsentrictestservice.service.LoadTestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoadTestController {
    private final LoadTestService loadTestService;

    public LoadTestController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }

    @PostMapping("/load-test")
    public LoadTestResponse loadTest(@RequestBody LoadTestRequest request) {
        return loadTestService.run(request);
    }
}
