package com.stratocloud.repository;

import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.script.ScriptDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScriptDefinitionRepository extends ControllableRepository<ScriptDefinition> {
    Page<ScriptDefinition> page(String search, List<Long> tenantIds, List<Long> ownerIds, Boolean disabled, Pageable pageable);

    List<ScriptDefinition> findAllEnabled();
}
