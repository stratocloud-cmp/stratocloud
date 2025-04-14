package com.stratocloud.repository;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.exceptions.ResourceNotFoundException;
import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class ResourceRepositoryImpl extends AbstractControllableRepository<Resource, ResourceJpaRepository>
        implements ResourceRepository{

    protected ResourceRepositoryImpl(ResourceJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource findResource(Long resourceId) {
        return jpaRepository.findById(resourceId).orElseThrow(
                () -> new ResourceNotFoundException("Resource not found by id: %s".formatted(resourceId))
        );
    }

    @Override
    @Transactional
    public Resource lockResource(Long resourceId) {
        return jpaRepository.findByIdIs(resourceId).orElseThrow(
                () -> new EntityNotFoundException("Resource not found by id: %s.".formatted(resourceId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAllByFilters(ResourceFilters filters) {
        Specification<Resource> spec = createSpecByFilters(filters);
        return jpaRepository.findAll(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Resource> page(ResourceFilters filters, Pageable pageable) {
        Specification<Resource> spec = createSpecByFilters(filters);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Resource> pageUnclaimed(String category, String search, List<Long> resourceIds, Pageable pageable) {
        Specification<Resource> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(category))
            spec = spec.and(getCategorySpec(List.of(category)));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(resourceIds)) {
            spec = spec.and(getIdSpec(resourceIds));
        } else {
            spec = spec.and(getOwnerSpec(List.of(BuiltInIds.SYSTEM_USER_ID)));
            spec = spec.and(getVisibleStateSpec());
        }


        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Resource> createSpecByFilters(ResourceFilters filters) {
        Specification<Resource> spec = getCallingTenantSpec();

        if(Utils.isEmpty(CallContext.current().getCallingUser().grantedTags()))
            spec = spec.and(getCallingOwnerSpec().or(getPublicSpec()));
        else
            spec = spec.and(getCallingOwnerTagsSpec().or(getCallingOwnerSpec()).or(getPublicSpec()));

        if(Utils.isNotBlank(filters.search()))
            spec = spec.and(getSearchSpec(filters.search()));

        if(filters.recycled() != null)
            spec = spec.and(getRecycledSpec(filters.recycled()));


        if(Utils.isNotEmpty(filters.resourceIds())){
            spec = spec.and(getIdSpec(filters.resourceIds()));
        } else {
            if(Utils.isEmpty(filters.states()))
                spec = spec.and(getVisibleStateSpec());
            else
                spec = spec.and(getStatesSpec(filters.states()));
            
            if(Utils.isNotEmpty(filters.syncStates()))
                spec = spec.and(getSyncStatesSpec(filters.syncStates()));
        }

        if(Utils.isNotEmpty(filters.requirementTargetIds()))
            spec = spec.and(getRequirementTargetSpec(filters.resourceTypes(), filters.requirementTargetIds()));

        if(Utils.isNotEmpty(filters.tenantIds()))
            spec = spec.and(getTenantSpec(filters.tenantIds()));

        if(Utils.isNotEmpty(filters.ownerIds()))
            spec = spec.and(getOwnerSpec(filters.ownerIds()));

        if(Utils.isNotEmpty(filters.providerIds()))
            spec = spec.and(getProviderSpec(filters.providerIds()));

        if(Utils.isNotEmpty(filters.accountIds()))
            spec = spec.and(getAccountSpec(filters.accountIds()));

        if(Utils.isNotEmpty(filters.resourceCategories()))
            spec = spec.and(getCategorySpec(filters.resourceCategories()));

        if(Utils.isNotEmpty(filters.resourceTypes()))
            spec = spec.and(getResourceTypeSpec(filters.resourceTypes()));

        if(Utils.isNotEmpty(filters.tagsMap()))
            spec = spec.and(getTagSpec(filters.tagsMap()));

        if(filters.ipPoolAttachable() != null)
            spec = spec.and(getIpPoolAttachableSpec(filters.ipPoolAttachable()));

        return spec;
    }

    private Specification<Resource> getSyncStatesSpec(List<ResourceSyncState> syncStates) {
        return (root, query, criteriaBuilder) -> root.get("syncState").in(syncStates);
    }

    private Specification<Resource> getPublicSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("publicResource"), true);
    }

    private Specification<Resource> getIpPoolAttachableSpec(boolean ipPoolAttachable) {
        Set<String> attachableTypeIds = new HashSet<>();
        Set<String> notAttachableTypeIds = new HashSet<>();

        for (Provider provider : ProviderRegistry.getProviders()) {
            for (ResourceHandler resourceHandler : provider.getResourceHandlers()) {
                if (resourceHandler.canAttachIpPool())
                    attachableTypeIds.add(resourceHandler.getResourceTypeId());
                else
                    notAttachableTypeIds.add(resourceHandler.getResourceTypeId());
            }
        }

        if(ipPoolAttachable)
            return (root, query, criteriaBuilder) -> root.get("type").in(attachableTypeIds);
        else
            return (root, query, criteriaBuilder) -> root.get("type").in(notAttachableTypeIds);
    }

    private Specification<Resource> getRequirementTargetSpec(List<String> resourceTypes,
                                                             List<Long> requirementTargetIds) {
        if(Utils.isEmpty(requirementTargetIds))
            return getSpec();

        List<Resource> targetCandidates = findAllById(requirementTargetIds);

        List<Long> selfIds = new ArrayList<>();

        if(Utils.isNotEmpty(resourceTypes))
            selfIds.addAll(
                    targetCandidates.stream().filter(
                            resource -> resourceTypes.contains(resource.getType())
                    ).map(Resource::getId).toList()
            );


        Map<String, List<Long>> targetIdsMap = getTargetIdsMap(resourceTypes, targetCandidates);


        Specification<Resource> specification = (root, query, criteriaBuilder) -> {
            Objects.requireNonNull(query).distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            for(String resourceType:targetIdsMap.keySet()){
                List<Long> targetIds = targetIdsMap.get(resourceType);
                Join<Relationship, Resource> join = root.join("requirements");
                Predicate predicate = join.get("target").get("id").in(targetIds);
                predicates.add(predicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        if(Utils.isNotEmpty(selfIds)){
            return specification.and(getIdSpec(selfIds));
        }else {
            return specification;
        }
    }

    private static Map<String, List<Long>> getTargetIdsMap(List<String> resourceTypes, List<Resource> targetCandidates) {
        final Map<String, List<Long>> targetIdsMap = new HashMap<>();
        for (Resource targetCandidate : targetCandidates) {
            boolean isValidTarget;
            if(Utils.isNotEmpty(resourceTypes)){
                isValidTarget = resourceTypes.stream().anyMatch(
                        rt -> ProviderRegistry.getResourceHandler(rt).canConnectTo(targetCandidate.getResourceHandler())
                );
            }else {
                isValidTarget = true;
            }

            if(isValidTarget){
                targetIdsMap.computeIfAbsent(targetCandidate.getType(), k->new ArrayList<>());
                targetIdsMap.get(targetCandidate.getType()).add(targetCandidate.getId());
            }
        }
        return targetIdsMap;
    }

    private Specification<Resource> getCallingOwnerTagsSpec() {
        if(CallContext.current().isAdmin())
            return getSpec();

        return getTagSpec(CallContext.current().getCallingUser().grantedTags());
    }

    private Specification<Resource> getStatesSpec(List<ResourceState> states) {
        return (root, query, criteriaBuilder) -> root.get("state").in(states);
    }

    private Specification<Resource> getVisibleStateSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("state"), ResourceState.DESTROYED);
    }

    private Specification<Resource> getRecycledSpec(boolean recycled) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("recycled"), recycled);
    }


    private Specification<Resource> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Objects.requireNonNull(query).distinct(true);

            Join<RuntimeProperty, Resource> runtimePropertyJoin = root.join(
                    "runtimeProperties", JoinType.LEFT
            );

            Predicate p1 = criteriaBuilder.like(runtimePropertyJoin.get("value"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(runtimePropertyJoin.get("valueName"), "%" + search + "%");
            Predicate p3 = criteriaBuilder.equal(runtimePropertyJoin.get("searchable"), true);

            Predicate p4 = criteriaBuilder.and(criteriaBuilder.or(p1, p2), p3);

            Predicate p5 = criteriaBuilder.like(root.get("name"), "%" + search + "%");
            Predicate p6 = criteriaBuilder.like(root.get("description"), "%" + search + "%");

            return criteriaBuilder.or(p4, p5, p6);
        };
    }

    private Specification<Resource> getProviderSpec(List<String> providerIds) {
        return (root, query, criteriaBuilder) -> root.get("providerId").in(providerIds);
    }

    private Specification<Resource> getAccountSpec(List<Long> accountIds) {
        return (root, query, criteriaBuilder) -> root.get("accountId").in(accountIds);
    }

    private Specification<Resource> getCategorySpec(List<String> resourceCategories) {
        return (root, query, criteriaBuilder) -> root.get("category").in(resourceCategories);
    }

    private Specification<Resource> getResourceTypeSpec(List<String> resourceTypes) {
        return (root, query, criteriaBuilder) -> root.get("type").in(resourceTypes);
    }

    private Specification<Resource> getTagSpec(Map<String, List<String>> tagsMap) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (String tagKey : tagsMap.keySet()) {
                Join<ResourceTag, Resource> tagJoin = root.join(
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






    @Override
    @Transactional(readOnly = true)
    public long countByFilters(ResourceFilters resourceFilters) {
        Specification<Resource> spec = createSpecByFilters(resourceFilters);
        return jpaRepository.count(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean existsByExternalResource(ExternalResource externalResource) {
        return jpaRepository.existsByAccountIdAndTypeAndExternalIdAndStateIn(
                externalResource.accountId(),
                externalResource.type(),
                externalResource.externalId(),
                ResourceState.getVisibleStates()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Resource> findByExternalResource(ExternalResource externalResource) {
        return jpaRepository.findByAccountIdAndTypeAndExternalIdAndStateIn(
                externalResource.accountId(),
                externalResource.type(),
                externalResource.externalId(),
                ResourceState.getVisibleStates()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countByName(String name) {
        return jpaRepository.countByName(name);
    }

    @Override
    public void validatePermission(Resource entity) {
        UserSession callingUser = CallContext.current().getCallingUser();
        Map<String, List<String>> grantedTags = callingUser.grantedTags();

        if(Utils.isEmpty(grantedTags)) {
            super.validatePermission(entity);
            return;
        }

        if(Objects.equals(callingUser.userId(), entity.getOwnerId()))
            return;

        boolean allTagsMatched = grantedTags.entrySet().stream().allMatch(
                entry -> entry.getValue().stream().anyMatch(
                        v -> entity.hasTag(entry.getKey(), v)
                )
        );

        if(allTagsMatched)
            return;

        super.validatePermission(entity);
    }
}
