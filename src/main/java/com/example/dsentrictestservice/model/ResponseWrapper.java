package com.example.dsentrictestservice.model;

public record ResponseWrapper<T>(T data, long executionTimeMs, String language) {
}
