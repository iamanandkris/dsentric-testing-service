package com.example.dsentrictestservice.core;

public enum EntityKind {
    USER("users"),
    PRODUCT("products"),
    ORDER("orders");

    private final String tableName;

    EntityKind(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
