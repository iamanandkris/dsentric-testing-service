package com.example.dsentrictestservice.dsentric;

import io.dsentric.JvmContract;
import io.dsentric.ValidationResult;
import io.dsentric.annotations.contract;
import io.dsentric.annotations.decodable;
import io.dsentric.annotations.email;
import io.dsentric.annotations.internal;
import io.dsentric.annotations.masked;
import io.dsentric.annotations.nonEmpty;
import io.dsentric.annotations.reserved;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DsentricJvmJavaInteropTest {
    @Test
    void javaRecordContractShouldDecodeNestedRecordsAndJavaLists() {
        JvmContract<JavaProductPayload> contract = JvmContract.ofRecord(JavaProductPayload.class);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sku", "JAVA-001");
        payload.put("name", "Java Product");
        payload.put("description", "Nested decode check");
        payload.put("category", "test");
        payload.put("price", Map.of("amount", 19.99d, "currency", "USD"));
        payload.put("inventory", Map.of("available", 10, "reserved", 2));
        payload.put("tags", List.of("java", "nested"));

        assertDoesNotThrow(() -> {
            ValidationResult<JavaProductPayload> result = contract.validate(payload);
            assertTrue(result.isValid(), "expected nested Java record validation to succeed");
            JavaProductPayload value = result.getValue().orElseThrow();
            assertEquals("JAVA-001", value.sku());
            assertEquals(10, value.inventory().available());
            assertEquals(List.of("java", "nested"), value.tags());
        });
    }

    @Test
    void javaRecordContractShouldRejectNestedReservedField() {
        JvmContract<JavaUserPayload> contract = JvmContract.ofRecord(JavaUserPayload.class);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", "reserved@example.com");
        payload.put("name", "Reserved User");
        payload.put("preferences", Map.of("internalSegment", "secret"));

        ValidationResult<JavaUserPayload> result = contract.validate(payload);

        assertFalse(result.isValid(), "expected nested reserved field to be rejected");
        assertTrue(
                result.getErrors().stream().anyMatch(v -> v.getPath().contains("internalSegment")),
                "expected a violation mentioning internalSegment"
        );
    }

    @Test
    void javaRecordContractShouldMaskAndDropNestedSensitiveFieldsOnSanitize() {
        JvmContract<JavaOrderPayload> contract = JvmContract.ofRecord(JavaOrderPayload.class);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", 1L);
        payload.put("orderNumber", "ORD-JAVA-001");
        payload.put("status", "pending");
        payload.put("items", List.of(Map.of("productId", 1L, "sku", "SKU-1", "quantity", 2, "unitPrice", 10.0d)));
        payload.put("totals", Map.of("subtotal", 20.0d, "tax", 2.0d, "shipping", 1.0d, "total", 23.0d));
        payload.put("paymentInfo", Map.of("method", "credit_card", "last4", "1234", "gatewayReference", "gw-123"));

        Map<String, Object> sanitized = contract.sanitize(payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> paymentInfo = (Map<String, Object>) sanitized.get("paymentInfo");

        assertNotNull(paymentInfo, "expected nested paymentInfo to survive sanitize");
        assertEquals("****", paymentInfo.get("last4"), "expected nested masked field to be masked");
        assertFalse(paymentInfo.containsKey("gatewayReference"), "expected nested internal field to be removed");
    }

    @decodable
    @contract
    public record JavaPrice(Double amount, String currency) {
    }

    @decodable
    @contract
    public record JavaInventory(Integer available, Integer reserved) {
    }

    @decodable
    @contract
    public record JavaPreferences(@reserved Optional<String> internalSegment) {
    }

    @decodable
    @contract
    public record JavaOrderItem(Long productId, String sku, Integer quantity, Double unitPrice) {
    }

    @decodable
    @contract
    public record JavaTotals(Double subtotal, Double tax, Double shipping, Double total) {
    }

    @decodable
    @contract
    public record JavaPaymentInfo(
            String method,
            @masked("****") String last4,
            @internal Optional<String> gatewayReference
    ) {
    }

    @contract
    public record JavaProductPayload(
            String sku,
            @nonEmpty String name,
            String description,
            String category,
            JavaPrice price,
            JavaInventory inventory,
            List<String> tags
    ) {
    }

    @contract
    public record JavaUserPayload(
            @email String email,
            @nonEmpty String name,
            JavaPreferences preferences
    ) {
    }

    @contract
    public record JavaOrderPayload(
            Long userId,
            String orderNumber,
            String status,
            List<JavaOrderItem> items,
            JavaTotals totals,
            JavaPaymentInfo paymentInfo
    ) {
    }
}
