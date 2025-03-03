package com.stratocloud.repository;

import com.stratocloud.ip.IpAddress;
import com.stratocloud.ip.ManagedIp;
import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ManagedIpJpaRepository extends AuditableJpaRepository<ManagedIp>, JpaSpecificationExecutor<ManagedIp> {
    List<ManagedIp> findByIpPoolIdAndAddressIn(Long ipPoolId, List<IpAddress> addresses);
}
