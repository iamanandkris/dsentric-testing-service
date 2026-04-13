package com.example.dsentrictestservice.core;

import com.example.dsentrictestservice.model.ServiceResult;

import java.util.List;
import java.util.Map;

public abstract class AbstractCrudService implements CrudService {
    private final GenericCrudEngine engine;
    private final EntityKind entityKind;
    private final String language;
    private final String endpointBase;

    protected AbstractCrudService(GenericCrudEngine engine, EntityKind entityKind, String language, String endpointBase) {
        this.engine = engine;
        this.entityKind = entityKind;
        this.language = language;
        this.endpointBase = endpointBase;
    }

    @Override
    public ServiceResult<Map<String, Object>> create(Map<String, Object> payload) {
        return engine.create(entityKind, language, endpointBase, payload);
    }

    @Override
    public ServiceResult<Map<String, Object>> read(long id) {
        return engine.read(entityKind, language, endpointBase, id);
    }

    @Override
    public ServiceResult<Map<String, Object>> update(long id, Map<String, Object> payload) {
        return engine.update(entityKind, language, endpointBase, id, payload);
    }

    @Override
    public ServiceResult<Map<String, Object>> patch(long id, Map<String, Object> payload) {
        return engine.patch(entityKind, language, endpointBase, id, payload);
    }

    @Override
    public ServiceResult<Void> delete(long id) {
        return engine.delete(entityKind, language, endpointBase, id);
    }

    @Override
    public ServiceResult<List<Map<String, Object>>> list() {
        return engine.list(entityKind, language, endpointBase);
    }

    @Override
    public String language() {
        return language;
    }
}
