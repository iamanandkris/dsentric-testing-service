package com.example.dsentrictestservice.service;

import com.example.dsentrictestservice.core.EntityKind;
import com.example.dsentrictestservice.model.SeedResponse;
import com.example.dsentrictestservice.repository.JsonStoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class SeedDataService implements ApplicationRunner {
    private final JsonStoreRepository repository;
    private final ContractFacade contractFacade;
    private final int userCount;
    private final int productCount;
    private final int orderCount;
    private final Random random = new Random(42);

    public SeedDataService(
            JsonStoreRepository repository,
            ContractFacade contractFacade,
            @Value("${app.seed.users}") int userCount,
            @Value("${app.seed.products}") int productCount,
            @Value("${app.seed.orders}") int orderCount
    ) {
        this.repository = repository;
        this.contractFacade = contractFacade;
        this.userCount = userCount;
        this.productCount = productCount;
        this.orderCount = orderCount;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count(EntityKind.USER) == 0) {
            reseed();
        }
    }

    public SeedResponse reseed() {
        repository.truncateAll();
        List<Long> users = new ArrayList<>(userCount);
        List<Long> products = new ArrayList<>(productCount);

        for (int i = 0; i < userCount; i++) {
            Map<String, Object> user = contractFacade.validateUser(sampleUser(i), "java");
            users.add(repository.create(EntityKind.USER, user, null));
        }
        for (int i = 0; i < productCount; i++) {
            Map<String, Object> product = contractFacade.validateProduct(sampleProduct(i), "java");
            products.add(repository.create(EntityKind.PRODUCT, product, null));
        }
        for (int i = 0; i < orderCount; i++) {
            Map<String, Object> order = contractFacade.validateOrder(sampleOrder(i, users, products), "java");
            repository.create(EntityKind.ORDER, order, ((Number) order.get("userId")).longValue());
        }
        return new SeedResponse(userCount, productCount, orderCount);
    }

    private Map<String, Object> sampleUser(int idx) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("email", "user" + idx + "@example.com");
        user.put("name", "User " + idx);
        if (idx % 2 == 0) {
            user.put("age", 18 + (idx % 50));
        }
        if (idx % 5 == 0) {
            user.put("address", Map.of("street", idx + " Main St", "city", "City " + (idx % 20), "zipCode", String.format("%05d", 10000 + idx)));
        }
        user.put("preferences", Map.of("newsletter", idx % 2 == 0, "notifications", idx % 3 == 0));
        user.put("registeredAt", Instant.now().minusSeconds(idx * 864L).toString());
        user.put("internalNotes", "seed-note-" + idx);
        return user;
    }

    private Map<String, Object> sampleProduct(int idx) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("sku", "SKU-" + String.format("%04d", idx));
        product.put("name", "Product " + idx);
        product.put("description", "Generated product " + idx);
        product.put("category", switch (idx % 4) {
            case 0 -> "electronics";
            case 1 -> "clothing";
            case 2 -> "home";
            default -> "books";
        });
        product.put("price", Map.of("amount", 1.0 + (idx % 999), "currency", "USD"));
        int available = random.nextInt(500);
        int reserved = available == 0 ? 0 : random.nextInt(available + 1);
        product.put("inventory", Map.of("available", available, "reserved", reserved));
        product.put("tags", List.of("seeded", idx % 3 == 0 ? "featured" : "catalog"));
        product.put("createdAt", Instant.now().minusSeconds(idx * 360L).toString());
        product.put("internalCost", Math.round((0.5 + (idx % 200) * 0.37) * 100.0) / 100.0);
        return product;
    }

    private Map<String, Object> sampleOrder(int idx, List<Long> users, List<Long> products) {
        long userId = users.get(random.nextInt(users.size()));
        int itemCount = 1 + random.nextInt(3);
        List<Map<String, Object>> items = new ArrayList<>();
        double subtotal = 0.0;
        for (int i = 0; i < itemCount; i++) {
            long productId = products.get(random.nextInt(products.size()));
            int quantity = 1 + random.nextInt(3);
            double price = 10.0 + random.nextInt(120);
            subtotal += quantity * price;
            items.add(Map.of(
                    "productId", productId,
                    "sku", "SKU-" + String.format("%04d", productId),
                    "quantity", quantity,
                    "unitPrice", price
            ));
        }
        double tax = Math.round(subtotal * 0.1 * 100.0) / 100.0;
        double shipping = subtotal > 100 ? 0.0 : 10.0;
        double total = subtotal + tax + shipping;

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("userId", userId);
        order.put("orderNumber", "ORD-" + String.format("%05d", idx));
        order.put("status", idx % 10 == 0 ? "cancelled" : (idx % 3 == 0 ? "pending" : "completed"));
        order.put("items", items);
        order.put("totals", Map.of("subtotal", subtotal, "tax", tax, "shipping", shipping, "total", total));
        order.put("shippingAddress", Map.of("street", idx + " Market St", "city", "Ship City", "zipCode", "90210"));
        order.put("paymentInfo", Map.of(
                "method", "credit_card",
                "last4", String.format("%04d", idx % 10000),
                "gatewayReference", "gw-seed-" + idx
        ));
        order.put("placedAt", Instant.now().minusSeconds(idx * 120L).toString());
        return order;
    }
}
