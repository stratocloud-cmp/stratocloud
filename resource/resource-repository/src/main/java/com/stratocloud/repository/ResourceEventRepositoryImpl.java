package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.resource.event.ResourceEvent;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceEventRepositoryImpl
        extends AbstractControllableRepository<ResourceEvent, ResourceEventJpaRepository>
        implements ResourceEventRepository {
    public ResourceEventRepositoryImpl(ResourceEventJpaRepository jpaRepository) {
        super(jpaRepository);
    }

}
