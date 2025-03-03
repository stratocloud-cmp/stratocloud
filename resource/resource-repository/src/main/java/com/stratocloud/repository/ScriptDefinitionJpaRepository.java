package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.script.ScriptDefinition;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScriptDefinitionJpaRepository
        extends ControllableJpaRepository<ScriptDefinition>, JpaSpecificationExecutor<ScriptDefinition> {
}
