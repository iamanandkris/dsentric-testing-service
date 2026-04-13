package com.example.dsentrictestservice.service.jvm;

import java.util.Map;

public interface JvmLanguageContractFacade {
    Map<String, Object> validateUser(Map<String, Object> payload, String language);

    Map<String, Object> validateProduct(Map<String, Object> payload, String language);

    Map<String, Object> validateOrder(Map<String, Object> payload, String language);

    Map<String, Object> patchUser(Map<String, Object> current, Map<String, Object> patch, String language);

    Map<String, Object> patchProduct(Map<String, Object> current, Map<String, Object> patch, String language);

    Map<String, Object> patchOrder(Map<String, Object> current, Map<String, Object> patch, String language);

    Map<String, Object> sanitizeUser(Map<String, Object> payload);

    Map<String, Object> sanitizeProduct(Map<String, Object> payload);

    Map<String, Object> sanitizeOrder(Map<String, Object> payload);

    Map<String, Object> userSchema();

    Map<String, Object> productSchema();

    Map<String, Object> orderSchema();

    Map<String, Object> validateUserDraft(Map<String, Object> payload, String language);

    Map<String, Object> validateProductDraft(Map<String, Object> payload, String language);

    Map<String, Object> validateOrderDraft(Map<String, Object> payload, String language);
}
