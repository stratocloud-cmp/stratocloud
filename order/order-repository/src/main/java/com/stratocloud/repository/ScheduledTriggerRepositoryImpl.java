package com.stratocloud.repository;


import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.job.ScheduledTrigger;
import com.stratocloud.jpa.repository.AbstractRepository;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ScheduledTriggerRepositoryImpl
        extends AbstractRepository<ScheduledTrigger, String, ScheduledTriggerJpaRepository>
        implements ScheduledTriggerRepository{

    public ScheduledTriggerRepositoryImpl(ScheduledTriggerJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTrigger> findByNextTriggerTimeBefore(LocalDateTime dateTime) {
        return jpaRepository.findByNextTriggerTimeBefore(dateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTrigger findTrigger(String triggerId) {
        return findById(triggerId).orElseThrow(
                () -> new EntityNotFoundException("Trigger not found by id: " + triggerId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledTrigger> page(String search, Pageable pageable) {
        Specification<ScheduledTrigger> spec = getSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ScheduledTrigger> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("triggerId"), "%"+search+"%");
    }
}
