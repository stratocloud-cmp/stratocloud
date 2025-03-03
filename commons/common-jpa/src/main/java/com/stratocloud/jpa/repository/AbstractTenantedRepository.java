package com.stratocloud.jpa.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.RoleType;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.PermissionNotGrantedException;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractTenantedRepository<E extends Tenanted, R extends TenantedJpaRepository<E>>
        extends AbstractAuditableRepository<E, R> implements TenantedRepository<E>{

    private static final Set<RoleType> permittedRoleTypes = Set.of(
            RoleType.SUPER_ADMIN, RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN
    );

    protected boolean isPublicToSubTenants(){
        return true;
    }

    @Override
    public void validatePermission(E entity) {
        UserSession callingUser = CallContext.current().getCallingUser();

        if(!permittedRoleTypes.contains(callingUser.roleType()))
            throw new PermissionNotGrantedException(
                    "Calling user %s's role type %s can never have write permissions to %s entities.".formatted(
                            callingUser.loginName(), callingUser.roleType(), getEntityClass().getSimpleName()
                    )
            );

        if(callingUser.roleType() == RoleType.SUPER_ADMIN)
            return;

        Set<Long> permittedTenantIds = getPermittedTenantIds();

        if(!permittedTenantIds.contains(entity.getTenantId()))
            throw new PermissionNotGrantedException(
                    "Calling user %s can never have write permissions to %s entities of tenant %s.".formatted(
                            callingUser.loginName(), getEntityClass().getSimpleName(), entity.getTenantId()
                    )
            );
    }

    private Set<Long> getPermittedTenantIds() {
        UserSession callingUser = CallContext.current().getCallingUser();

        Set<Long> permittedTenantIds = new HashSet<>();

        permittedTenantIds.addAll(callingUser.grantedTenantIds());
        permittedTenantIds.addAll(callingUser.subTenantIds());

        if(isPublicToSubTenants())
            permittedTenantIds.addAll(callingUser.inheritedTenantIds());

        return permittedTenantIds;
    }

    protected AbstractTenantedRepository(R jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<E> findAllByTenantIds(List<Long> tenantIds) {
        return jpaRepository.findByTenantIdIn(tenantIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTenantIds(List<Long> tenantIds) {
        return jpaRepository.existsByTenantIdIn(tenantIds);
    }


    protected Specification<E> getCallingTenantSpec(){
        if(CallContext.current().isSuperAdmin())
            return getSpec();

        Set<Long> tenantIds = getPermittedTenantIds();
        return getTenantSpec(tenantIds);
    }

    protected Specification<E> getTenantSpec(Collection<Long> tenantIds) {
        return (root, query, criteriaBuilder) -> root.get("tenantId").in(tenantIds);
    }
}
