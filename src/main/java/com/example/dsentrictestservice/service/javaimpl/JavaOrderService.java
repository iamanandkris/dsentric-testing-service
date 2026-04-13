package com.example.dsentrictestservice.service.javaimpl;

import com.example.dsentrictestservice.core.AbstractCrudService;
import com.example.dsentrictestservice.core.EntityKind;
import com.example.dsentrictestservice.core.GenericCrudEngine;
import org.springframework.stereotype.Service;

@Service
public class JavaOrderService extends AbstractCrudService {
    public JavaOrderService(GenericCrudEngine engine) {
        super(engine, EntityKind.ORDER, "java", "/orders-java");
    }
}
