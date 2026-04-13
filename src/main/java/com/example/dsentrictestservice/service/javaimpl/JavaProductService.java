package com.example.dsentrictestservice.service.javaimpl;

import com.example.dsentrictestservice.core.AbstractCrudService;
import com.example.dsentrictestservice.core.EntityKind;
import com.example.dsentrictestservice.core.GenericCrudEngine;
import org.springframework.stereotype.Service;

@Service
public class JavaProductService extends AbstractCrudService {
    public JavaProductService(GenericCrudEngine engine) {
        super(engine, EntityKind.PRODUCT, "java", "/products-java");
    }
}
