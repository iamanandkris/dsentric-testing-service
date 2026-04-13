package com.example.dsentrictestservice.controller;

import com.example.dsentrictestservice.core.CrudService;
import com.example.dsentrictestservice.model.ResponseWrapper;
import com.example.dsentrictestservice.model.ServiceResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public abstract class BaseEntityController {
    protected ResponseEntity<ResponseWrapper<Map<String, Object>>> created(CrudService service, Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wrap(service.create(payload), service.language()));
    }

    protected ResponseEntity<ResponseWrapper<Map<String, Object>>> read(CrudService service, long id) {
        return ResponseEntity.ok(wrap(service.read(id), service.language()));
    }

    protected ResponseEntity<ResponseWrapper<Map<String, Object>>> updated(CrudService service, long id, Map<String, Object> payload) {
        return ResponseEntity.ok(wrap(service.update(id, payload), service.language()));
    }

    protected ResponseEntity<ResponseWrapper<Map<String, Object>>> patched(CrudService service, long id, Map<String, Object> payload) {
        return ResponseEntity.ok(wrap(service.patch(id, payload), service.language()));
    }

    protected ResponseEntity<Void> deleted(CrudService service, long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    protected ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listed(CrudService service) {
        return ResponseEntity.ok(wrap(service.list(), service.language()));
    }

    private <T> ResponseWrapper<T> wrap(ServiceResult<T> result, String language) {
        return new ResponseWrapper<>(result.data(), result.executionTimeMs(), language);
    }
}
