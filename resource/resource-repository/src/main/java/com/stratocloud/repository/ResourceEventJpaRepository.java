package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.resource.event.ResourceEvent;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResourceEventJpaRepository
        extends ControllableJpaRepository<ResourceEvent>, JpaSpecificationExecutor<ResourceEvent> {
}
