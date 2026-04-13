package com.example.dsentrictestservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DsentricTestingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DsentricTestingServiceApplication.class, args);
    }
}
