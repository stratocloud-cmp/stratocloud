package com.stratocloud.repository;

import com.stratocloud.ip.*;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public class ManagedIpRepositoryImpl extends AbstractAuditableRepository<ManagedIp, ManagedIpJpaRepository>
        implements ManagedIpRepository {

    private final IpPoolRepositoryImpl ipPoolRepository;

    public ManagedIpRepositoryImpl(ManagedIpJpaRepository jpaRepository,
                                   IpPoolRepositoryImpl ipPoolRepository) {
        super(jpaRepository);
        this.ipPoolRepository = ipPoolRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedIp> findManagedIps(Long ipPoolId, List<String> addresses) {
        if(Utils.isEmpty(addresses))
            return new ArrayList<>();

        return jpaRepository.findByIpPoolIdAndAddressIn(
                ipPoolId,
                addresses.stream().map(IpAddress::new).toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedIp> provideAvailableIps(Long ipPoolId, int ipNumber){
        Specification<ManagedIp> spec = getSpec();

        if(ipPoolId != null)
            spec = spec.and(getIpPoolSpec(List.of(ipPoolId)));

        spec = spec.and(getStateSpec(Set.of(ManagedIpState.AVAILABLE)));

        PageRequest pageRequest = PageRequest.of(
                0,
                ipNumber,
                Sort.by(Sort.Direction.ASC,"toBigInteger")
        );

        return jpaRepository.findAll(spec, pageRequest).getContent();
    }

    private Specification<ManagedIp> getStateSpec(Collection<ManagedIpState> states) {
        return (root, query, criteriaBuilder) -> root.get("state").in(states);
    }

    @Override
    public void validatePermission(ManagedIp entity) {
        ipPoolRepository.validatePermission(entity.getRange().getIpPool());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagedIp> page(Long ipPoolId,
                                Long networkResourceId,
                                InternetProtocol protocol,
                                List<String> ips,
                                String search,
                                Pageable pageable) {
        Specification<ManagedIp> spec = getSpec();

        if(ipPoolId != null)
            spec = spec.and(getIpPoolSpec(List.of(ipPoolId)));

        spec = spec.and(getNetworkResourceAndProtocolSpec(networkResourceId, protocol));

        if(Utils.isNotEmpty(ips))
            spec = spec.and(getIpSpec(ips));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ManagedIp> getIpSpec(List<String> ips) {
        return (root, query, criteriaBuilder) -> root.get("address").in(ips);
    }

    private Specification<ManagedIp> getNetworkResourceAndProtocolSpec(Long networkResourceId,
                                                                       InternetProtocol protocol) {
        if(networkResourceId == null && protocol == null)
            return getSpec();

        List<Long> networkResourceIds = networkResourceId != null ? List.of(networkResourceId) : null;

        IpPoolFilters filters = new IpPoolFilters(null, networkResourceIds, null, protocol);

        List<Long> ipPoolIds = ipPoolRepository.filter(filters).stream().map(IpPool::getId).toList();

        return getIpPoolSpec(ipPoolIds);
    }

    private Specification<ManagedIp> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String s = "%" + search + "%";
            return criteriaBuilder.like(root.get("address").as(String.class), s);
        };
    }

    private Specification<ManagedIp> getIpPoolSpec(Collection<Long> ipPoolIds) {
        return (root, query, criteriaBuilder) -> root.get("ipPoolId").in(ipPoolIds);
    }
}
