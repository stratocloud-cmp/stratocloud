package com.stratocloud.repository;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.resource.event.ResourceEvent;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ResourceEventRepositoryImpl
        extends AbstractControllableRepository<ResourceEvent, ResourceEventJpaRepository>
        implements ResourceEventRepository {
    public ResourceEventRepositoryImpl(ResourceEventJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceEvent> page(String search,
                                    List<Long> resourceIds,
                                    List<String> eventTypes,
                                    List<StratoEventLevel> eventLevels,
                                    List<StratoEventSource> eventSources,
                                    Pageable pageable) {
        Specification<ResourceEvent> spec = getCallingOwnerSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(resourceIds))
            spec = spec.and(getResourceIdSpec(resourceIds));

        if(Utils.isNotEmpty(eventTypes))
            spec = spec.and(getEventTypeSpec(eventTypes));

        if(Utils.isNotEmpty(eventLevels))
            spec = spec.and(getEventLevelSpec(eventLevels));

        if(Utils.isNotEmpty(eventSources))
            spec = spec.and(getEventSourceSpec(eventSources));

        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceEvent> findAllByResourceId(Long resourceId) {
        return jpaRepository.findAll(
                getSpec().and(getResourceIdSpec(List.of(resourceId)))
        );
    }

    private Specification<ResourceEvent> getEventSourceSpec(List<StratoEventSource> eventSources) {
        return (root, query, criteriaBuilder)
                -> root.get("source").in(eventSources);
    }

    private Specification<ResourceEvent> getEventLevelSpec(List<StratoEventLevel> eventLevels) {
        return (root, query, criteriaBuilder)
                -> root.get("level").in(eventLevels);
    }


    private Specification<ResourceEvent> getEventTypeSpec(List<String> eventTypes) {
        return (root, query, criteriaBuilder)
                -> root.get("eventType").in(eventTypes);
    }

    private Specification<ResourceEvent> getResourceIdSpec(List<Long> resourceIds) {
        return (root, query, criteriaBuilder)
                -> root.get("resourceId").in(resourceIds);
    }

    private Specification<ResourceEvent> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("eventType"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("eventTypeName"), "%" + search + "%");
            Predicate p3 = criteriaBuilder.like(root.get("internalEventId"), "%" + search + "%");
            Predicate p4 = criteriaBuilder.like(root.get("externalEventId"), "%" + search + "%");
            Predicate p5 = criteriaBuilder.like(root.get("summary"), "%" + search + "%");
            Predicate p6 = criteriaBuilder.like(root.get("resourceName"), "%" + search + "%");
            return criteriaBuilder.or(p1,p2,p3,p4,p5,p6);
        };
    }
}
