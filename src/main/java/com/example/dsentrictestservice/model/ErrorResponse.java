package com.example.dsentrictestservice.model;

import java.util.List;

public record ErrorResponse(String error, List<String> details, String language) {
}
