package com.stratocloud.repository;

import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.ManagedIp;
import com.stratocloud.jpa.repository.AuditableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ManagedIpRepository extends AuditableRepository<ManagedIp> {
    List<ManagedIp> findManagedIps(Long ipPoolId, List<String> addresses);

    List<ManagedIp> provideAvailableIps(Long ipPoolId, int ipNumber);

    Page<ManagedIp> page(Long ipPoolId,
                         Long networkResourceId,
                         InternetProtocol protocol,
                         List<String> ips,
                         String search,
                         Pageable pageable);
}
