package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpPool;
import com.stratocloud.ip.IpPoolFilters;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class IpPoolRepositoryImpl extends AbstractTenantedRepository<IpPool, IpPoolJpaRepository>
        implements IpPoolRepository {
    public IpPoolRepositoryImpl(IpPoolJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public IpPool findIpPool(Long ipPoolId) {
        return jpaRepository.findById(ipPoolId).orElseThrow(
                () -> new EntityNotFoundException("IP pool not found.")
        );
    }


    @Override
    @Transactional(readOnly = true)
    public Page<IpPool> page(IpPoolFilters filters, Pageable pageable) {
        Specification<IpPool> spec = createIpPoolSpec(filters);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<IpPool> createIpPoolSpec(IpPoolFilters filters) {
        Specification<IpPool> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(filters.ipPoolIds()))
            spec = spec.and(getIdSpec(filters.ipPoolIds()));

        if(Utils.isNotEmpty(filters.networkResourceIds()))
            spec = spec.and(getNetworkResourceSpec(filters.networkResourceIds()));

        if(Utils.isNotBlank(filters.search()))
            spec = spec.and(getSearchSpec(filters.search()));

        if(filters.protocol() != null)
            spec = spec.and(getProtocolSpec(filters.protocol()));
        return spec;
    }

    @Override
    public List<IpPool> filter(IpPoolFilters filters) {
        Specification<IpPool> spec = createIpPoolSpec(filters);
        return jpaRepository.findAll(spec);
    }

    private Specification<IpPool> getProtocolSpec(InternetProtocol protocol) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("protocol"), protocol);
    }

    private Specification<IpPool> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            Predicate p1 = criteriaBuilder.like(root.get("name"), s);
            Predicate p2 = criteriaBuilder.like(root.get("cidr"), s);
            Predicate p3 = criteriaBuilder.like(root.get("gateway"), s);
            return criteriaBuilder.or(p1, p2, p3);
        };
    }

    private Specification<IpPool> getNetworkResourceSpec(List<Long> networkResourceIds) {
        return (root, query, criteriaBuilder) -> {
            Join<Resource, IpPool> join = root.join("attachedNetworkResources");
            return join.get("id").in(networkResourceIds);
        };
    }
}
