package com.stratocloud.repository;

import com.stratocloud.ip.IpPool;
import com.stratocloud.ip.IpPoolFilters;
import com.stratocloud.jpa.repository.TenantedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IpPoolRepository extends TenantedRepository<IpPool> {
    IpPool findIpPool(Long ipPoolId);

    Page<IpPool> page(IpPoolFilters filters, Pageable pageable);

    List<IpPool> filter(IpPoolFilters filters);
}
