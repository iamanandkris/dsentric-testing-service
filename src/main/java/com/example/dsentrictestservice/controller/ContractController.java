package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.service.ContractFacade;
import com.example.dsentrictestservice.service.jvm.JvmLanguageContractFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/contracts")
public class ContractController {
    private final ContractFacade scalaContractFacade;
    private final JvmLanguageContractFacade javaContractFacade;
    private final JvmLanguageContractFacade kotlinContractFacade;

    public ContractController(
            ContractFacade scalaContractFacade,
            @Qualifier("javaContractFacade") JvmLanguageContractFacade javaContractFacade,
            @Qualifier("kotlinContractFacade") JvmLanguageContractFacade kotlinContractFacade
    ) {
        this.scalaContractFacade = scalaContractFacade;
        this.javaContractFacade = javaContractFacade;
        this.kotlinContractFacade = kotlinContractFacade;
    }

    @GetMapping("/{entity}/schema")
    public Map<String, Object> schema(
            @PathVariable String entity,
            @RequestParam(defaultValue = "scala") String language
    ) {
        return switch (language) {
            case "scala" -> schemaFor(entity, scalaContractFacade);
            case "java" -> schemaFor(entity, javaContractFacade);
            case "kotlin" -> schemaFor(entity, kotlinContractFacade);
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    @PostMapping("/{entity}/draft/{language}")
    public Map<String, Object> validateDraft(
            @PathVariable String entity,
            @PathVariable String language,
            @RequestBody Map<String, Object> payload
    ) {
        return switch (language) {
            case "scala" -> draftFor(entity, payload, language, scalaContractFacade);
            case "java" -> draftFor(entity, payload, language, javaContractFacade);
            case "kotlin" -> draftFor(entity, payload, language, kotlinContractFacade);
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private Map<String, Object> schemaFor(String entity, ContractFacade facade) {
        return switch (entity) {
            case "users" -> facade.userSchema();
            case "products" -> facade.productSchema();
            case "orders" -> facade.orderSchema();
            default -> throw new IllegalArgumentException("Unknown contract entity: " + entity);
        };
    }

    private Map<String, Object> schemaFor(String entity, JvmLanguageContractFacade facade) {
        return switch (entity) {
            case "users" -> facade.userSchema();
            case "products" -> facade.productSchema();
            case "orders" -> facade.orderSchema();
            default -> throw new IllegalArgumentException("Unknown contract entity: " + entity);
        };
    }

    private Map<String, Object> draftFor(String entity, Map<String, Object> payload, String language, ContractFacade facade) {
        return switch (entity) {
            case "users" -> facade.validateUserDraft(payload, language);
            case "products" -> facade.validateProductDraft(payload, language);
            case "orders" -> facade.validateOrderDraft(payload, language);
            default -> throw new IllegalArgumentException("Unknown contract entity: " + entity);
        };
    }

    private Map<String, Object> draftFor(String entity, Map<String, Object> payload, String language, JvmLanguageContractFacade facade) {
        return switch (entity) {
            case "users" -> facade.validateUserDraft(payload, language);
            case "products" -> facade.validateProductDraft(payload, language);
            case "orders" -> facade.validateOrderDraft(payload, language);
            default -> throw new IllegalArgumentException("Unknown contract entity: " + entity);
        };
    }
}
