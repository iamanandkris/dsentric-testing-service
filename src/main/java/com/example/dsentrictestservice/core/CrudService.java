package com.example.dsentrictestservice.core;

import com.example.dsentrictestservice.model.ServiceResult;

import java.util.List;
import java.util.Map;

public interface CrudService {
    ServiceResult<Map<String, Object>> create(Map<String, Object> payload);

    ServiceResult<Map<String, Object>> read(long id);

    ServiceResult<Map<String, Object>> update(long id, Map<String, Object> payload);

    ServiceResult<Map<String, Object>> patch(long id, Map<String, Object> payload);

    ServiceResult<Void> delete(long id);

    ServiceResult<List<Map<String, Object>>> list();

    String language();
}
