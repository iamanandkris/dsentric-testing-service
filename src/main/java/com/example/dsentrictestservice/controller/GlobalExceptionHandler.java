package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.exception.NotFoundException;
import com.example.dsentrictestservice.exception.ValidationException;
import com.example.dsentrictestservice.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), ex.getDetails(), ex.getLanguage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), List.of(ex.getMessage()), ex.getLanguage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {}", request.getRequestURI(), ex);
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error", List.of(message), languageFromPath(request.getRequestURI())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request for {}", request.getRequestURI(), ex);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Bad request", List.of(ex.getMessage()), languageFromPath(request.getRequestURI())));
    }

    private String languageFromPath(String path) {
        if (path.endsWith("-scala") || path.contains("-scala/")) {
            return "scala";
        }
        if (path.endsWith("-kotlin") || path.contains("-kotlin/")) {
            return "kotlin";
        }
        return "java";
    }
}
