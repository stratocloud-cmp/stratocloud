package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.script.SoftwareDefinition;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class SoftwareDefinitionRepositoryImpl
        extends AbstractControllableRepository<SoftwareDefinition, SoftwareDefinitionJpaRepository>
        implements SoftwareDefinitionRepository {

    public SoftwareDefinitionRepositoryImpl(SoftwareDefinitionJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SoftwareDefinition> page(String search,
                                         List<Long> tenantIds,
                                         List<Long> ownerIds,
                                         Boolean disabled,
                                         Pageable pageable) {
        Specification<SoftwareDefinition> spec = getCallingTenantSpec();

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

    private Specification<SoftwareDefinition> getDisabledSpec(boolean disabled) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("disabled"), disabled);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDefinitionKey(String definitionKey) {
        return jpaRepository.existsByDefinitionKey(definitionKey);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SoftwareDefinition> findAllEnabled(){
        return jpaRepository.findAll(getDisabledSpec(false));
    }

    private Specification<SoftwareDefinition> getSearchSpec(String search) {
        String s = "%" + search + "%";
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("name"), s);
            Predicate p2 = criteriaBuilder.like(root.get("definitionKey"), s);
            return criteriaBuilder.or(p1, p2);
        };
    }

    private Specification<SoftwareDefinition> getPublicDefinitionSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("publicDefinition"), true);
    }
}
