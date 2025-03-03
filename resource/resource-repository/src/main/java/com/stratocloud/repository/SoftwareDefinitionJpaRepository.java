package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.script.SoftwareDefinition;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SoftwareDefinitionJpaRepository
        extends ControllableJpaRepository<SoftwareDefinition>, JpaSpecificationExecutor<SoftwareDefinition> {
    boolean existsByDefinitionKey(String definitionKey);
}
