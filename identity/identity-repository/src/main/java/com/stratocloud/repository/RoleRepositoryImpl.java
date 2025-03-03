package com.stratocloud.repository;

import com.stratocloud.identity.RoleType;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.role.Role;
import com.stratocloud.role.RoleFilters;
import com.stratocloud.user.UserRole;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class RoleRepositoryImpl extends AbstractTenantedRepository<Role, RoleJpaRepository> implements RoleRepository {

    public RoleRepositoryImpl(RoleJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Role findRole(Long roleId) {
        return jpaRepository.findById(roleId).orElseThrow(
                () -> new EntityNotFoundException("Role not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Role> page(RoleFilters filters, Pageable pageable) {
        Specification<Role> spec = getRoleSpecification(filters);
        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Role> getRoleSpecification(RoleFilters filters) {
        List<Long> roleIds = filters.roleIds();
        List<RoleType> roleTypes = filters.roleTypes();
        List<Long> userIds = filters.userIds();
        String search = filters.search();

        Specification<Role> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(roleIds))
            spec = spec.and(getIdSpec(roleIds));

        if(Utils.isNotEmpty(roleTypes))
            spec = spec.and(getRoleTypeSpec(roleTypes));

        if(Utils.isNotEmpty(userIds))
            spec = spec.and(getUserSpec(userIds));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return spec;
    }

    private Specification<Role> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            return criteriaBuilder.like(root.get("name"), s);
        };
    }

    private Specification<Role> getUserSpec(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            Join<UserRole, Role> join = root.join("userRoles");
            return join.get("user").get("id").in(userIds);
        };
    }

    private Specification<Role> getRoleTypeSpec(List<RoleType> roleTypes) {
        return (root, query, criteriaBuilder) -> root.get("type").in(roleTypes);
    }
}
