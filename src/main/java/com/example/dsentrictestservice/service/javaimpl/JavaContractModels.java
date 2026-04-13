package com.example.dsentrictestservice.service.javaimpl;

import io.dsentric.JvmContract;
import io.dsentric.annotations.*;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JavaContractModels {
    private JavaContractModels() {
    }

    @decodable
    @contract
    public record Address(
            Optional<String> street,
            Optional<String> city,
            Optional<String> zipCode
    ) {
    }

    @decodable
    @contract
    public record Preferences(
            Optional<Boolean> newsletter,
            Optional<Boolean> notifications,
            @reserved Optional<String> internalSegment
    ) {
    }

    @contract
    public record UserPayload(
            @email String email,
            @nonEmpty String name,
            @min(0) Optional<Integer> age,
            Optional<Address> address,
            Optional<Preferences> preferences,
            @immutable Optional<String> registeredAt,
            @internal Optional<String> internalNotes
    ) {
    }

    @decodable
    @contract
    public record Price(
            Double amount,
            String currency
    ) {
    }

    @decodable
    @contract
    public record Inventory(
            Integer available,
            Integer reserved
    ) {
    }

    public static final class InventoryValidator implements io.dsentric.ContractValidator<ProductPayload> {
        @Override
        public scala.collection.immutable.List<String> validate(ProductPayload value) {
            Inventory inventory = value.inventory().orElse(null);
            Integer available = readInt(inventory, "available");
            Integer reserved = readInt(inventory, "reserved");
            if (available != null && reserved != null && reserved > available) {
                return CollectionConverters.asScala(java.util.List.of("inventory.reserved must not exceed inventory.available")).toList();
            }
            return CollectionConverters.asScala(java.util.List.<String>of()).toList();
        }
    }

    @validateContract({InventoryValidator.class})
    @contract
    public record ProductPayload(
            String sku,
            @nonEmpty String name,
            String description,
            String category,
            Price price,
            Optional<Inventory> inventory,
            List<String> tags,
            @immutable Optional<String> createdAt,
            @internal Optional<Double> internalCost
    ) {
    }

    @decodable
    @contract
    public record OrderItem(
            Integer productId,
            String sku,
            @positive Integer quantity,
            Double unitPrice
    ) {
    }

    @decodable
    @contract
    public record Totals(
            Optional<Double> subtotal,
            Optional<Double> tax,
            Optional<Double> shipping,
            Double total
    ) {
    }

    @decodable
    @contract
    public record PaymentInfo(
            String method,
            @masked("****") String last4,
            @internal Optional<String> gatewayReference
    ) {
    }

    public static final class OrderTotalsValidator implements io.dsentric.ContractValidator<OrderPayload> {
        @Override
        public scala.collection.immutable.List<String> validate(OrderPayload value) {
            if (value.items() == null || value.totals() == null) {
                return CollectionConverters.asScala(java.util.List.<String>of()).toList();
            }
            double computedSubtotal = value.items().stream()
                    .mapToDouble(item -> {
                        Integer quantity = readInt(item, "quantity");
                        Double unitPrice = readDouble(item, "unitPrice");
                        if (quantity == null || unitPrice == null) {
                            return 0.0d;
                        }
                        return quantity * unitPrice;
                    })
                    .sum();
            Double subtotalValue = readDouble(value.totals(), "subtotal");
            Double taxValue = readDouble(value.totals(), "tax");
            Double shippingValue = readDouble(value.totals(), "shipping");
            Double totalValue = readDouble(value.totals(), "total");
            double subtotal = subtotalValue == null ? computedSubtotal : subtotalValue;
            double tax = taxValue == null ? 0.0d : taxValue;
            double shipping = shippingValue == null ? 0.0d : shippingValue;
            double expected = subtotal + tax + shipping;
            ArrayList<String> errors = new ArrayList<>();
            if (Math.abs(subtotal - computedSubtotal) > 0.01d) {
                errors.add("totals.subtotal must equal the sum of line items");
            }
            if (totalValue != null && Math.abs(totalValue - expected) > 0.01d) {
                errors.add("totals.total must equal subtotal + tax + shipping");
            }
            return CollectionConverters.asScala(errors).toList();
        }
    }

    @validateContract({OrderTotalsValidator.class})
    @contract
    public record OrderPayload(
            Integer userId,
            @immutable String orderNumber,
            String status,
            @nonEmpty List<OrderItem> items,
            Totals totals,
            Optional<Address> shippingAddress,
            Optional<PaymentInfo> paymentInfo,
            Optional<String> placedAt,
            @reserved Optional<String> internalStatus
    ) {
    }

    private static Integer readInt(Inventory source, String key) {
        if (source == null) {
            return null;
        }
        return switch (key) {
            case "available" -> source.available();
            case "reserved" -> source.reserved();
            default -> null;
        };
    }

    private static Integer readInt(OrderItem source, String key) {
        if (source == null) {
            return null;
        }
        return switch (key) {
            case "quantity" -> source.quantity();
            default -> null;
        };
    }

    private static Double readDouble(Price source, String key) {
        if (source == null) {
            return null;
        }
        return switch (key) {
            case "amount" -> source.amount();
            default -> null;
        };
    }

    private static Double readDouble(OrderItem source, String key) {
        if (source == null) {
            return null;
        }
        return switch (key) {
            case "unitPrice" -> source.unitPrice();
            default -> null;
        };
    }

    private static Double readDouble(Totals source, String key) {
        if (source == null) {
            return null;
        }
        return switch (key) {
            case "subtotal" -> source.subtotal().orElse(null);
            case "tax" -> source.tax().orElse(null);
            case "shipping" -> source.shipping().orElse(null);
            case "total" -> source.total();
            default -> null;
        };
    }

    public static final JvmContract<UserPayload> USER_CONTRACT = JvmContract.ofRecord(UserPayload.class);
    public static final JvmContract<ProductPayload> PRODUCT_CONTRACT = JvmContract.ofRecord(ProductPayload.class);
    public static final JvmContract<OrderPayload> ORDER_CONTRACT = JvmContract.ofRecord(OrderPayload.class);
}
