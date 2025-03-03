package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.limit.ResourceUsageLimit;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ResourceUsageLimitRepositoryImpl
        extends AbstractTenantedRepository<ResourceUsageLimit, ResourceUsageLimitJpaRepository>
        implements ResourceUsageLimitRepository {

    public ResourceUsageLimitRepositoryImpl(ResourceUsageLimitJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    @Transactional(readOnly = true)
    public ResourceUsageLimit findLimit(Long limitId) {
        return findById(limitId).orElseThrow(
                () -> new EntityNotFoundException("Resource usage limit not found by id "+limitId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceUsageLimit> page(List<Long> tenantIds, String search, Pageable pageable) {
        Specification<ResourceUsageLimit> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(tenantIds))
            spec = spec.and(getTenantSpec(tenantIds));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ResourceUsageLimit> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%"+search+"%");
    }
}
