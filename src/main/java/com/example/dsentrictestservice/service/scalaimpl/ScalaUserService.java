package com.example.dsentrictestservice.service.scalaimpl;

import com.example.dsentrictestservice.core.AbstractCrudService;
import com.example.dsentrictestservice.core.EntityKind;
import com.example.dsentrictestservice.core.GenericCrudEngine;
import org.springframework.stereotype.Service;

@Service
public class ScalaUserService extends AbstractCrudService {
    public ScalaUserService(GenericCrudEngine engine) {
        super(engine, EntityKind.USER, "scala", "/users-scala");
    }
}
