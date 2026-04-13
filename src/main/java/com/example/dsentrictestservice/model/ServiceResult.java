package com.example.dsentrictestservice.model;

public record ServiceResult<T>(T data, long executionTimeMs, boolean success) {
}
