package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.group.UserGroup;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.user.User;
import com.stratocloud.user.UserFilters;
import com.stratocloud.user.UserRole;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl extends AbstractTenantedRepository<User, UserJpaRepository> implements UserRepository {

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    public List<User> findAllByFilters(UserFilters userFilters) {
        Specification<User> spec = getUserSpecification(userFilters);

        return jpaRepository.findAll(spec);
    }

    @Override
    public Page<User> page(UserFilters userFilters, Pageable pageable) {
        Specification<User> spec = getUserSpecification(userFilters);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<User> getUserSpecification(UserFilters userFilters) {
        Specification<User> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(userFilters.tenantIds()))
            spec = spec.and(getTenantSpec(userFilters.tenantIds()));

        if(Utils.isNotEmpty(userFilters.userIds()))
            spec = spec.and(getIdSpec(userFilters.userIds()));

        if(Utils.isNotEmpty(userFilters.roleIds()))
            spec = spec.and(getRoleSpec(userFilters.roleIds()));

        if(Utils.isNotEmpty(userFilters.userGroupIds()))
            spec = spec.and(getGroupSpec(userFilters.userGroupIds()));

        if(Utils.isNotBlank(userFilters.search()))
            spec = spec.and(getSearchSpec(userFilters.search()));

        if(userFilters.disabled() != null)
            spec = spec.and(getDisabledSpec(userFilters.disabled()));

        if(userFilters.locked() != null)
            spec = spec.and(getLockedSpec(userFilters.locked()));

        return spec;
    }

    private Specification<User> getGroupSpec(List<Long> userGroupIds) {
        return (root, query, criteriaBuilder) -> {
            Join<UserGroup, User> join = root.join("groups");
            return join.get("id").in(userGroupIds);
        };
    }

    private Specification<User> getRoleSpec(List<Long> roleIds) {
        return (root, query, criteriaBuilder) -> {
            Join<UserRole, User> join = root.join("userRoles");
            return join.get("role").get("id").in(roleIds);
        };
    }

    private Specification<User> getLockedSpec(boolean locked) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("locked"), locked);
    }

    private Specification<User> getDisabledSpec(boolean disabled) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("disabled"), disabled);
    }

    private Specification<User> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            Predicate p1 = criteriaBuilder.like(root.get("loginName"), s);
            Predicate p2 = criteriaBuilder.like(root.get("realName"), s);
            Predicate p3 = criteriaBuilder.like(root.get("emailAddress"), s);
            Predicate p4 = criteriaBuilder.like(root.get("phoneNumber"), s);
            return criteriaBuilder.or(p1, p2, p3, p4);
        };
    }

    @Override
    public User findUser(Long userId) {
        return jpaRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User not found.")
        );
    }

    @Override
    public Optional<User> findByLoginName(String loginName) {
        return jpaRepository.findByLoginName(loginName);
    }
}
