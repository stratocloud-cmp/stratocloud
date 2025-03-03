package com.stratocloud.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.tenant.TenantFilters;
import com.stratocloud.utils.GraphUtil;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class TenantRepositoryImpl extends AbstractAuditableRepository<Tenant, TenantJpaRepository>
        implements TenantRepository {

    public TenantRepositoryImpl(TenantJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> findInheritedTenants(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        List<Tenant> tenants = GraphUtil.bfs(
                tenant,
                t -> t.getParent()!=null ? List.of(t.getParent()) : List.of()
        );
        tenants.remove(tenant);
        return tenants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> findSubTenants(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        List<Tenant> tenants = GraphUtil.bfs(tenant, Tenant::getChildren);
        tenants.remove(tenant);
        return tenants;
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant findTenant(Long tenantId) {
        return jpaRepository.findById(tenantId).orElseThrow(
                () -> new EntityNotFoundException("Tenant not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tenant> page(TenantFilters tenantFilters, Pageable pageable) {

        Specification<Tenant> spec = getTenantSpecification(tenantFilters);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Tenant> getTenantSpecification(TenantFilters tenantFilters) {
        Specification<Tenant> spec = getSpec();

        if(!CallContext.current().isSuperAdmin())
            spec = spec.and(getIdSpec(getVisibleTenantIds(tenantFilters.includeInherited())));

        if(Utils.isNotEmpty(tenantFilters.tenantIds()))
            spec = spec.and(getIdSpec(tenantFilters.tenantIds()));

        if(Utils.isNotBlank(tenantFilters.search()))
            spec = spec.and(getSearchSpec(tenantFilters.search()));

        if(tenantFilters.disabled() != null)
            spec = spec.and(getDisabledSpec(tenantFilters.disabled()));

        if(Utils.isNotEmpty(tenantFilters.parentIds()))
            spec = spec.and(getParentSpec(tenantFilters.parentIds()));


        return spec;
    }

    private Specification<Tenant> getDisabledSpec(Boolean disabled) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("disabled"), disabled);
    }

    private Specification<Tenant> getParentSpec(List<Long> parentIds) {
        return (root, query, criteriaBuilder) -> root.get("parentId").in(parentIds);
    }

    private Specification<Tenant> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            return criteriaBuilder.like(root.get("name"), s);
        };
    }

    private Set<Long> getVisibleTenantIds(Boolean includeInherited) {
        UserSession callingUser = CallContext.current().getCallingUser();
        Set<Long> permittedTenantIds = new HashSet<>();
        permittedTenantIds.addAll(callingUser.grantedTenantIds());
        permittedTenantIds.addAll(callingUser.subTenantIds());

        if(includeInherited)
            permittedTenantIds.addAll(callingUser.inheritedTenantIds());

        return permittedTenantIds;
    }

    @Override
    public List<Tenant> findVisibleRoots(boolean includeInherited) {
        Specification<Tenant> spec = getSpec();

        if(!CallContext.current().isSuperAdmin())
            spec = spec.and(getIdSpec(getVisibleTenantIds(includeInherited)));

        List<Tenant> visibleAll = jpaRepository.findAll(spec);

        Set<Long> visibleIds = visibleAll.stream().map(Tenant::getId).collect(Collectors.toSet());

        List<Tenant> visibleRoots = new ArrayList<>();

        for (Tenant tenant : visibleAll)
            if(tenant.getParent() == null || !visibleIds.contains(tenant.getParent().getId()))
                visibleRoots.add(tenant);

        return visibleRoots;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }
}
