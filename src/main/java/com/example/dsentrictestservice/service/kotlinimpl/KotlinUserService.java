package com.example.dsentrictestservice.service.kotlinimpl;

import com.example.dsentrictestservice.core.AbstractCrudService;
import com.example.dsentrictestservice.core.EntityKind;
import com.example.dsentrictestservice.core.GenericCrudEngine;
import org.springframework.stereotype.Service;

@Service
public class KotlinUserService extends AbstractCrudService {
    public KotlinUserService(GenericCrudEngine engine) {
        super(engine, EntityKind.USER, "kotlin", "/users-kotlin");
    }
}
