package com.example.dsentrictestservice.service.javaimpl;

import com.example.dsentrictestservice.service.jvm.JvmContractSupport;
import com.example.dsentrictestservice.service.jvm.JvmLanguageContractFacade;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JavaContractFacade implements JvmLanguageContractFacade {
    public Map<String, Object> validateUser(Map<String, Object> payload, String language) {
        return JvmContractSupport.validate(payload, JavaContractModels.USER_CONTRACT, language);
    }

    public Map<String, Object> validateProduct(Map<String, Object> payload, String language) {
        return JvmContractSupport.validate(payload, JavaContractModels.PRODUCT_CONTRACT, language);
    }

    public Map<String, Object> validateOrder(Map<String, Object> payload, String language) {
        return JvmContractSupport.validate(payload, JavaContractModels.ORDER_CONTRACT, language);
    }

    public Map<String, Object> patchUser(Map<String, Object> current, Map<String, Object> patch, String language) {
        Map<String, Object> merged = deepMerge(current, patch);
        return JvmContractSupport.validatePatch(current, patch, merged, JavaContractModels.USER_CONTRACT, language);
    }

    public Map<String, Object> patchProduct(Map<String, Object> current, Map<String, Object> patch, String language) {
        return JvmContractSupport.validatePatch(current, patch, deepMerge(current, patch), JavaContractModels.PRODUCT_CONTRACT, language);
    }

    public Map<String, Object> patchOrder(Map<String, Object> current, Map<String, Object> patch, String language) {
        return JvmContractSupport.validatePatch(current, patch, deepMerge(current, patch), JavaContractModels.ORDER_CONTRACT, language);
    }

    public Map<String, Object> sanitizeUser(Map<String, Object> payload) {
        return JvmContractSupport.sanitize(payload, JavaContractModels.USER_CONTRACT);
    }

    public Map<String, Object> sanitizeProduct(Map<String, Object> payload) {
        return JvmContractSupport.sanitize(payload, JavaContractModels.PRODUCT_CONTRACT);
    }

    public Map<String, Object> sanitizeOrder(Map<String, Object> payload) {
        return JvmContractSupport.sanitize(payload, JavaContractModels.ORDER_CONTRACT);
    }

    public Map<String, Object> userSchema() {
        return JvmContractSupport.schema(JavaContractModels.USER_CONTRACT);
    }

    public Map<String, Object> productSchema() {
        return JvmContractSupport.schema(JavaContractModels.PRODUCT_CONTRACT);
    }

    public Map<String, Object> orderSchema() {
        return JvmContractSupport.schema(JavaContractModels.ORDER_CONTRACT);
    }

    public Map<String, Object> validateUserDraft(Map<String, Object> payload, String language) {
        return JvmContractSupport.validateDraft(payload, JavaContractModels.USER_CONTRACT, language);
    }

    public Map<String, Object> validateProductDraft(Map<String, Object> payload, String language) {
        return JvmContractSupport.validateDraft(payload, JavaContractModels.PRODUCT_CONTRACT, language);
    }

    public Map<String, Object> validateOrderDraft(Map<String, Object> payload, String language) {
        return JvmContractSupport.validateDraft(payload, JavaContractModels.ORDER_CONTRACT, language);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMerge(Map<String, Object> current, Map<String, Object> patch) {
        Map<String, Object> merged = new LinkedHashMap<>(current);
        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            Object existing = merged.get(entry.getKey());
            Object incoming = entry.getValue();
            if (existing instanceof Map<?, ?> existingMap && incoming instanceof Map<?, ?> incomingMap) {
                merged.put(entry.getKey(), deepMerge((Map<String, Object>) existingMap, (Map<String, Object>) incomingMap));
            } else {
                merged.put(entry.getKey(), incoming);
            }
        }
        return merged;
    }

}
