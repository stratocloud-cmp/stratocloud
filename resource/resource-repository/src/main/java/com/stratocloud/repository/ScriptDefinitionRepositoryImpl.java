package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.script.ScriptDefinition;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ScriptDefinitionRepositoryImpl
        extends AbstractControllableRepository<ScriptDefinition, ScriptDefinitionJpaRepository>
        implements ScriptDefinitionRepository {

    public ScriptDefinitionRepositoryImpl(ScriptDefinitionJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScriptDefinition> page(String search, List<Long> tenantIds, List<Long> ownerIds, Boolean disabled, Pageable pageable) {
        Specification<ScriptDefinition> spec = getCallingTenantSpec();

        spec = spec.and(getCallingOwnerSpec().or(getPublicDefinitionSpec()));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(tenantIds))
            spec = spec.and(getTenantSpec(tenantIds));

        if(Utils.isNotEmpty(ownerIds))
            spec = spec.and(getOwnerSpec(ownerIds));

        if(disabled != null)
            spec = spec.and(getDisabledSpec(disabled));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ScriptDefinition> getSearchSpec(String search) {
        String s = "%" + search + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), s);
    }

    private Specification<ScriptDefinition> getPublicDefinitionSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("publicDefinition"), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScriptDefinition> findAllEnabled() {
        return jpaRepository.findAll(getDisabledSpec(false));
    }

    private Specification<ScriptDefinition> getDisabledSpec(boolean disabled) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("disabled"), disabled);
    }
}
