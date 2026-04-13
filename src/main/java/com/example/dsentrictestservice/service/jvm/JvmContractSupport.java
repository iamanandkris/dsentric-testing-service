package com.example.dsentrictestservice.service.jvm;

import com.example.dsentrictestservice.exception.ValidationException;
import io.dsentric.JvmContract;
import io.dsentric.JvmViolation;
import io.dsentric.ValidationResult;

import java.util.List;
import java.util.Map;

public final class JvmContractSupport {
    private JvmContractSupport() {
    }

    public static <T> Map<String, Object> validate(Map<String, Object> payload, JvmContract<T> contract, String language) {
        ValidationResult<T> result = contract.validate(payload);
        if (!result.isValid()) {
            throw toValidationException(result.getErrors(), language);
        }
        return payload;
    }

    public static <T> Map<String, Object> validatePatch(
            Map<String, Object> current,
            Map<String, Object> patch,
            Map<String, Object> merged,
            JvmContract<T> contract,
            String language
    ) {
        ValidationResult<T> result = contract.validatePatch(current, patch);
        if (!result.isValid()) {
            throw toValidationException(result.getErrors(), language);
        }
        return merged;
    }

    public static <T> Map<String, Object> sanitize(Map<String, Object> payload, JvmContract<T> contract) {
        return contract.sanitize(payload);
    }

    public static <T> Map<String, Object> schema(JvmContract<T> contract) {
        return contract.jsonSchema();
    }

    public static <T> Map<String, Object> validateDraft(Map<String, Object> payload, JvmContract<T> contract, String language) {
        List<JvmViolation> violations = contract.validatePartial(payload);
        if (!violations.isEmpty()) {
            throw toValidationException(violations, language);
        }
        return payload;
    }

    public static ValidationException toValidationException(List<JvmViolation> violations, String language) {
        List<String> details = violations.stream()
                .map(v -> v.getPath() + ": " + v.getMessage())
                .toList();
        return new ValidationException("Validation failed", details, language);
    }
}
