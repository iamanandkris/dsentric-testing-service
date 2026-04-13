package com.example.dsentrictestservice.exception;

public class NotFoundException extends RuntimeException {
    private final String language;

    public NotFoundException(String message, String language) {
        super(message);
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
