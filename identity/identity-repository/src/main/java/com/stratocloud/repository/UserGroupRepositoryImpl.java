package com.stratocloud.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupTag;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.user.User;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class UserGroupRepositoryImpl extends AbstractTenantedRepository<UserGroup, UserGroupJpaRepository>
        implements UserGroupRepository {

    public UserGroupRepositoryImpl(UserGroupJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroup findUserGroup(Long userGroupId) {
        return jpaRepository.findById(userGroupId).orElseThrow(
                () -> new EntityNotFoundException("User group not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGroup> findByFilters(List<Long> userGroupIds,
                                         List<Long> userIds,
                                         String search,
                                         Map<String, List<String>> tagsMap,
                                         boolean allGroups) {
        Specification<UserGroup> spec = getUserGroupSpecification(userGroupIds, userIds, search, tagsMap, allGroups);

        return jpaRepository.findAll(spec);
    }

    private Specification<UserGroup> getUserGroupSpecification(List<Long> userGroupIds,
                                                               List<Long> userIds,
                                                               String search,
                                                               Map<String, List<String>> tagsMap,
                                                               Boolean allGroups) {
        boolean allGroupsEnabled = allGroups != null ? allGroups : false;

        Specification<UserGroup> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(userGroupIds))
            spec = spec.and(getIdSpec(userGroupIds));

        if(Utils.isNotEmpty(userIds))
            spec = spec.and(getMemberSpec(userIds));

        CallContext callContext = CallContext.current();

        if(!allGroupsEnabled && !callContext.isAdmin()) {
            spec = spec.and(getMemberSpec(List.of(callContext.getCallingUser().userId())));
        }

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(tagsMap))
            spec = spec.and(getTagSpec(tagsMap));

        return spec;
    }

    private Specification<UserGroup> getTagSpec(Map<String, List<String>> tagsMap) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (String tagKey : tagsMap.keySet()) {
                Join<UserGroupTag, UserGroup> tagJoin = root.join(
                        "tags", JoinType.LEFT
                );

                List<String> tagValues = tagsMap.get(tagKey);

                Predicate p1 = criteriaBuilder.equal(tagJoin.get("tagKey"), tagKey);
                Predicate p2 = tagJoin.get("tagValue").in(tagValues);

                predicates.add(p1);
                predicates.add(p2);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<UserGroup> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            Predicate p1 = criteriaBuilder.like(root.get("name"), s);
            Predicate p2 = criteriaBuilder.like(root.get("alias"), s);
            return criteriaBuilder.or(p1, p2);
        };
    }

    private Specification<UserGroup> getMemberSpec(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            Join<User, UserGroup> join = root.join("members");
            return join.get("id").in(userIds);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGroup> page(List<Long> userGroupIds,
                                List<Long> userIds,
                                String search,
                                Map<String, List<String>> tagsMap,
                                Boolean allGroups,
                                Pageable pageable) {
        Specification<UserGroup> spec = getUserGroupSpecification(userGroupIds, userIds, search, tagsMap, allGroups);
        return jpaRepository.findAll(spec, pageable);
    }


    @Override
    public void validatePermission(UserGroup entity) {
        if(entity.hasMember(CallContext.current().getCallingUser().userId()))
            return;

        super.validatePermission(entity);
    }
}
