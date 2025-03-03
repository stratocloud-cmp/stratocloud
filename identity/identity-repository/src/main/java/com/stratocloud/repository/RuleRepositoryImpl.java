package com.stratocloud.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.rule.Rule;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class RuleRepositoryImpl extends AbstractTenantedRepository<Rule, RuleJpaRepository> implements RuleRepository {

    private final TenantJpaRepository tenantJpaRepository;

    public RuleRepositoryImpl(RuleJpaRepository jpaRepository,
                              TenantJpaRepository tenantJpaRepository) {
        super(jpaRepository);
        this.tenantJpaRepository = tenantJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rule> findRuleByType(String type) {
        Long tenantId = CallContext.current().getCallingUser().tenantId();

        Optional<Rule> optional = jpaRepository.findByTypeAndTenantId(type, tenantId);

        Long currentTenantId = tenantId;

        while (optional.isEmpty()){

            Optional<Tenant> current = tenantJpaRepository.findById(currentTenantId);

            if(current.isEmpty())
                return Optional.empty();

            Tenant parent = current.get().getParent();
            if(parent == null)
                return Optional.empty();

            currentTenantId = parent.getId();
            optional = jpaRepository.findByTypeAndTenantId(type, currentTenantId);
        }

        return optional;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rule> page(List<Long> tenantIds, List<Long> ruleIds, List<String> ruleTypes, String search, Pageable pageable) {
        Specification<Rule> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(tenantIds))
            spec = spec.and(getTenantSpec(tenantIds));

        if(Utils.isNotEmpty(ruleIds))
            spec = spec.and(getIdSpec(ruleIds));

        if(Utils.isNotEmpty(ruleTypes))
            spec = spec.and(getTypeSpec(ruleTypes));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Rule> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            return criteriaBuilder.like(root.get("name"), s);
        };
    }

    private Specification<Rule> getTypeSpec(List<String> ruleTypes) {
        return (root, query, criteriaBuilder) -> root.get("type").in(ruleTypes);
    }


    @Override
    public Rule findRule(Long ruleId) {
        return findById(ruleId).orElseThrow(
                () -> new EntityNotFoundException("Rule not found.")
        );
    }
}
