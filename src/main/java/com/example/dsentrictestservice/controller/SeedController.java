package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.model.SeedResponse;
import com.example.dsentrictestservice.service.SeedDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeedController {
    private final SeedDataService seedDataService;

    public SeedController(SeedDataService seedDataService) {
        this.seedDataService = seedDataService;
    }

    @PostMapping("/seed")
    public SeedResponse reseed() {
        return seedDataService.reseed();
    }
}
