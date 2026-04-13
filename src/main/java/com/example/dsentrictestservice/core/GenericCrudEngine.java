package com.example.dsentrictestservice.core;

import com.example.dsentrictestservice.exception.NotFoundException;
import com.example.dsentrictestservice.exception.ValidationException;
import com.example.dsentrictestservice.model.ServiceResult;
import com.example.dsentrictestservice.repository.JsonStoreRepository;
import com.example.dsentrictestservice.service.MetricsLogger;
import com.example.dsentrictestservice.service.ContractFacade;
import com.example.dsentrictestservice.service.jvm.JvmLanguageContractFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class GenericCrudEngine {
    private static final int LIST_LIMIT = 100;

    private final JsonStoreRepository repository;
    private final ContractFacade scalaContractFacade;
    private final JvmLanguageContractFacade javaContractFacade;
    private final JvmLanguageContractFacade kotlinContractFacade;
    private final MetricsLogger metricsLogger;

    public GenericCrudEngine(
            JsonStoreRepository repository,
            ContractFacade scalaContractFacade,
            @Qualifier("javaContractFacade") JvmLanguageContractFacade javaContractFacade,
            @Qualifier("kotlinContractFacade") JvmLanguageContractFacade kotlinContractFacade,
            MetricsLogger metricsLogger
    ) {
        this.repository = repository;
        this.scalaContractFacade = scalaContractFacade;
        this.javaContractFacade = javaContractFacade;
        this.kotlinContractFacade = kotlinContractFacade;
        this.metricsLogger = metricsLogger;
    }

    public ServiceResult<Map<String, Object>> create(EntityKind kind, String language, String endpointBase, Map<String, Object> payload) {
        return execute(endpointBase, language, () -> {
            Map<String, Object> validated = validate(kind, payload, language);
            if (kind == EntityKind.ORDER) {
                validateOrderRelationships(validated, language);
            }
            Long userId = kind == EntityKind.ORDER ? extractOrderUserId(validated) : null;
            long id = repository.create(kind, validated, userId);
            return sanitizeExisting(kind, require(kind, id, language), language);
        });
    }

    public ServiceResult<Map<String, Object>> read(EntityKind kind, String language, String endpointBase, long id) {
        return execute(endpointBase + "/" + id, language, () -> sanitizeExisting(kind, require(kind, id, language), language));
    }

    public ServiceResult<Map<String, Object>> update(EntityKind kind, String language, String endpointBase, long id, Map<String, Object> payload) {
        return execute(endpointBase + "/" + id, language, () -> {
            require(kind, id, language);
            Map<String, Object> validated = validate(kind, payload, language);
            if (kind == EntityKind.ORDER) {
                validateOrderRelationships(validated, language);
            }
            repository.update(kind, id, validated, kind == EntityKind.ORDER ? extractOrderUserId(validated) : null);
            return sanitizeExisting(kind, require(kind, id, language), language);
        });
    }

    public ServiceResult<Map<String, Object>> patch(EntityKind kind, String language, String endpointBase, long id, Map<String, Object> payload) {
        return execute(endpointBase + "/" + id, language, () -> {
            Map<String, Object> current = require(kind, id, language);
            Map<String, Object> patched = patch(kind, current, payload, language);
            if (kind == EntityKind.ORDER) {
                validateOrderRelationships(patched, language);
            }
            repository.update(kind, id, patched, kind == EntityKind.ORDER ? extractOrderUserId(patched) : null);
            return sanitizeExisting(kind, require(kind, id, language), language);
        });
    }

    public ServiceResult<Void> delete(EntityKind kind, String language, String endpointBase, long id) {
        return execute(endpointBase + "/" + id, language, () -> {
            repository.delete(kind, id);
            return null;
        });
    }

    public ServiceResult<List<Map<String, Object>>> list(EntityKind kind, String language, String endpointBase) {
        return execute(endpointBase, language, () -> repository.list(kind, LIST_LIMIT).stream().map(row -> sanitizeExisting(kind, row, language)).toList());
    }

    private Map<String, Object> require(EntityKind kind, long id, String language) {
        Map<String, Object> found = repository.read(kind, id);
        if (found == null) {
            throw new NotFoundException(kind.name().toLowerCase() + " " + id + " not found", language);
        }
        return found;
    }

    private Map<String, Object> validate(EntityKind kind, Map<String, Object> payload, String language) {
        try {
            return switch (language) {
                case "scala" -> validateScala(kind, payload, language);
                case "java" -> validateJava(kind, payload, language);
                case "kotlin" -> validateKotlin(kind, payload, language);
                default -> throw new IllegalArgumentException("Unsupported language: " + language);
            };
        } catch (ValidationException ex) {
            if (!language.equals(ex.getLanguage())) {
                throw new ValidationException(ex.getMessage(), ex.getDetails(), language);
            }
            throw ex;
        }
    }

    private Map<String, Object> patch(EntityKind kind, Map<String, Object> current, Map<String, Object> patch, String language) {
        return switch (language) {
            case "scala" -> patchScala(kind, current, patch, language);
            case "java" -> patchJava(kind, current, patch, language);
            case "kotlin" -> patchKotlin(kind, current, patch, language);
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private Map<String, Object> sanitizeExisting(EntityKind kind, Map<String, Object> current, String language) {
        Map<String, Object> raw = stripId(current);
        Map<String, Object> sanitized = switch (language) {
            case "scala" -> sanitizeScala(kind, raw);
            case "java" -> sanitizeJava(kind, raw);
            case "kotlin" -> sanitizeKotlin(kind, raw);
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
        Map<String, Object> response = new java.util.LinkedHashMap<>(sanitized);
        response.put("id", current.get("id"));
        return response;
    }

    private Map<String, Object> stripId(Map<String, Object> source) {
        Map<String, Object> copy = new java.util.LinkedHashMap<>(source);
        copy.remove("id");
        return copy;
    }

    private void validateOrderRelationships(Map<String, Object> payload, String language) {
        Long userId = extractOrderUserId(payload);
        if (userId == null || !repository.exists(EntityKind.USER, userId)) {
            throw new ValidationException("Validation failed", List.of("field 'userId' must reference an existing user"), language);
        }
        List<Long> productIds = extractProductIds(payload.get("items"));
        Set<Long> existing = repository.existingIds(EntityKind.PRODUCT, productIds);
        List<String> missing = productIds.stream().filter(id -> !existing.contains(id)).distinct().map(id -> "productId " + id + " does not exist").toList();
        if (!missing.isEmpty()) {
            throw new ValidationException("Validation failed", missing, language);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractProductIds(Object itemsObj) {
        List<Long> productIds = new ArrayList<>();
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Object productId = itemMap.get("productId");
                    if (productId instanceof Number number) {
                        productIds.add(number.longValue());
                    }
                }
            }
        }
        return productIds;
    }

    private Long extractOrderUserId(Map<String, Object> payload) {
        Object userId = payload.get("userId");
        return userId instanceof Number number ? number.longValue() : null;
    }

    private Map<String, Object> validateScala(EntityKind kind, Map<String, Object> payload, String language) {
        return switch (kind) {
            case USER -> scalaContractFacade.validateUser(payload, language);
            case PRODUCT -> scalaContractFacade.validateProduct(payload, language);
            case ORDER -> scalaContractFacade.validateOrder(payload, language);
        };
    }

    private Map<String, Object> validateJava(EntityKind kind, Map<String, Object> payload, String language) {
        return switch (kind) {
            case USER -> javaContractFacade.validateUser(payload, language);
            case PRODUCT -> javaContractFacade.validateProduct(payload, language);
            case ORDER -> javaContractFacade.validateOrder(payload, language);
        };
    }

    private Map<String, Object> validateKotlin(EntityKind kind, Map<String, Object> payload, String language) {
        return switch (kind) {
            case USER -> kotlinContractFacade.validateUser(payload, language);
            case PRODUCT -> kotlinContractFacade.validateProduct(payload, language);
            case ORDER -> kotlinContractFacade.validateOrder(payload, language);
        };
    }

    private Map<String, Object> patchScala(EntityKind kind, Map<String, Object> current, Map<String, Object> patch, String language) {
        return switch (kind) {
            case USER -> scalaContractFacade.patchUser(stripId(current), patch, language);
            case PRODUCT -> scalaContractFacade.patchProduct(stripId(current), patch, language);
            case ORDER -> scalaContractFacade.patchOrder(stripId(current), patch, language);
        };
    }

    private Map<String, Object> patchJava(EntityKind kind, Map<String, Object> current, Map<String, Object> patch, String language) {
        return switch (kind) {
            case USER -> javaContractFacade.patchUser(stripId(current), patch, language);
            case PRODUCT -> javaContractFacade.patchProduct(stripId(current), patch, language);
            case ORDER -> javaContractFacade.patchOrder(stripId(current), patch, language);
        };
    }

    private Map<String, Object> patchKotlin(EntityKind kind, Map<String, Object> current, Map<String, Object> patch, String language) {
        return switch (kind) {
            case USER -> kotlinContractFacade.patchUser(stripId(current), patch, language);
            case PRODUCT -> kotlinContractFacade.patchProduct(stripId(current), patch, language);
            case ORDER -> kotlinContractFacade.patchOrder(stripId(current), patch, language);
        };
    }

    private Map<String, Object> sanitizeScala(EntityKind kind, Map<String, Object> raw) {
        return switch (kind) {
            case USER -> scalaContractFacade.sanitizeUser(raw);
            case PRODUCT -> scalaContractFacade.sanitizeProduct(raw);
            case ORDER -> scalaContractFacade.sanitizeOrder(raw);
        };
    }

    private Map<String, Object> sanitizeJava(EntityKind kind, Map<String, Object> raw) {
        return switch (kind) {
            case USER -> javaContractFacade.sanitizeUser(raw);
            case PRODUCT -> javaContractFacade.sanitizeProduct(raw);
            case ORDER -> javaContractFacade.sanitizeOrder(raw);
        };
    }

    private Map<String, Object> sanitizeKotlin(EntityKind kind, Map<String, Object> raw) {
        return switch (kind) {
            case USER -> kotlinContractFacade.sanitizeUser(raw);
            case PRODUCT -> kotlinContractFacade.sanitizeProduct(raw);
            case ORDER -> kotlinContractFacade.sanitizeOrder(raw);
        };
    }

    private <T> ServiceResult<T> execute(String endpoint, String language, Supplier<T> action) {
        long start = System.nanoTime();
        boolean success = false;
        try {
            T data = action.get();
            success = true;
            long elapsed = elapsed(start);
            return new ServiceResult<>(data, elapsed, true);
        } finally {
            metricsLogger.log(endpoint, language, elapsed(start), success);
        }
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
