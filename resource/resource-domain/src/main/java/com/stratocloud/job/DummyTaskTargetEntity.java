package com.stratocloud.job;

import com.stratocloud.identifier.SnowflakeId;

public class DummyTaskTargetEntity implements TaskTargetEntity {

    private final Long id = SnowflakeId.nextId();
    private final String entityDescription;

    public DummyTaskTargetEntity(String entityDescription) {
        this.entityDescription = entityDescription;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getEntityDescription() {
        return entityDescription;
    }
}
