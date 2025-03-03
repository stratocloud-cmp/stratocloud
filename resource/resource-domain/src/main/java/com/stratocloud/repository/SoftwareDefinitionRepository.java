package com.stratocloud.repository;

import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.script.SoftwareDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SoftwareDefinitionRepository extends ControllableRepository<SoftwareDefinition> {
    Page<SoftwareDefinition> page(String search, List<Long> tenantIds, List<Long> ownerIds, Boolean disabled, Pageable pageable);

    boolean existsByDefinitionKey(String definitionKey);

    @Transactional(readOnly = true)
    List<SoftwareDefinition> findAllEnabled();
}
