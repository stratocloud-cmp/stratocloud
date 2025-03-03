package com.stratocloud.repository;

import com.stratocloud.ip.IpPool;
import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IpPoolJpaRepository extends TenantedJpaRepository<IpPool>, JpaSpecificationExecutor<IpPool> {
}
