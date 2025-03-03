package com.stratocloud.ip;

import com.stratocloud.exceptions.AllocatingIpReachableException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.repository.IpPoolRepository;
import com.stratocloud.repository.ManagedIpRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class IpAllocator {

    private final IpPoolRepository ipPoolRepository;

    private final ManagedIpRepository ipRepository;

    private final PlatformTransactionManager transactionManager;

    public IpAllocator(IpPoolRepository ipPoolRepository,
                       ManagedIpRepository ipRepository,
                       PlatformTransactionManager transactionManager) {
        this.ipPoolRepository = ipPoolRepository;
        this.ipRepository = ipRepository;
        this.transactionManager = transactionManager;
    }


    @Transactional
    public synchronized Optional<IpPool> allocateIps(Resource networkResource,
                                                     InternetProtocol internetProtocol,
                                                     List<String> ips,
                                                     Resource usageResource) {
        return doAllocateIps(networkResource, internetProtocol, ips, usageResource, false);
    }

    @Transactional
    public synchronized void forceAllocateIps(Resource networkResource,
                                              InternetProtocol internetProtocol,
                                              List<String> ips,
                                              Resource usageResource) {
        doAllocateIps(networkResource, internetProtocol, ips, usageResource, true);
    }

    private List<IpPool> findIpPools(Resource networkResource,
                                     InternetProtocol internetProtocol){
        IpPoolFilters filters = new IpPoolFilters(
                null,
                List.of(networkResource.getId()),
                null,
                internetProtocol
        );

        return ipPoolRepository.filter(filters);
    }

    private Optional<IpPool> doAllocateIps(Resource networkResource,
                                           InternetProtocol internetProtocol,
                                           List<String> ips,
                                           Resource usageResource,
                                           boolean force) {
        if(Utils.isEmpty(ips))
            return Optional.empty();

        List<IpPool> ipPools = findIpPools(networkResource, internetProtocol);

        if(Utils.isEmpty(ipPools)){
            log.warn("Notice that no {} ip pool is currently attached to network resource {}, " +
                    "so ip addresses {} will not be managed by StratoCloud.",
                    internetProtocol, networkResource.getName(), ips);
            return Optional.empty();
        }

        if(ipPools.size()>1)
            throw new StratoException(
                    "There ara multiple ip pools attached to network resource %s.".formatted(networkResource.getName())
            );

        IpPool ipPool = ipPools.get(0);

        List<ManagedIp> managedIps = ipRepository.findManagedIps(ipPool.getId(), ips);

        if(Utils.isEmpty(managedIps)){
            log.warn("Ip pool {} does not have following ip addresses: {}.", ipPool.getName(), ips);
            return Optional.empty();
        }

        if(force){
            for (ManagedIp managedIp : managedIps) {
                managedIp.doAllocate(usageResource, null);
            }
            ipRepository.saveAll(managedIps);
        }else {
            for (ManagedIp managedIp : managedIps) {
                try {
                    managedIp.allocate(usageResource, null);
                } catch (AllocatingIpReachableException e){
                    managedIp.markExcluded(e.getMessage());
                    ipRepository.saveWithoutTransaction(managedIp);
                    throw e;
                }
            }

            ipRepository.saveAll(managedIps);
        }

        return Optional.of(ipPool);
    }


    @Transactional
    public void releaseIps(Resource networkResource,
                           InternetProtocol internetProtocol,
                           List<String> ips){
        if(Utils.isEmpty(ips))
            return;

        List<IpPool> ipPools = findIpPools(networkResource, internetProtocol);

        if(Utils.isEmpty(ipPools)){
            return;
        }

        IpPool ipPool = ipPools.get(0);

        List<ManagedIp> managedIps = ipRepository.findManagedIps(ipPool.getId(), ips);

        if(Utils.isEmpty(managedIps)){
            return;
        }

        managedIps.forEach(ManagedIp::release);

        ipRepository.saveAll(managedIps);
    }


    @DistributedLock(lockName = "AUTO_ALLOCATE_IP_LOCK", keyGenerator = AutoAllocateIpLockKeyGenerator.class)
    public Optional<ManagedIp> autoAllocateIp(Resource networkResource,
                                              InternetProtocol internetProtocol,
                                              Resource usageResource) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        return transactionTemplate.execute(
                status -> doAutoAllocateIp(networkResource, internetProtocol, usageResource)
        );
    }


    private Optional<ManagedIp> doAutoAllocateIp(Resource networkResource,
                                                 InternetProtocol internetProtocol,
                                                 Resource usageResource) {
        List<IpPool> ipPools = findIpPools(networkResource, internetProtocol);

        if(Utils.isEmpty(ipPools)){
            return Optional.empty();
        }

        if(ipPools.size()>1)
            throw new StratoException(
                    "There ara multiple ip pools attached to network resource %s.".formatted(networkResource.getName())
            );

        IpPool ipPool = ipPools.get(0);

        List<ManagedIp> managedIps = ipRepository.provideAvailableIps(ipPool.getId(), 1);

        if(Utils.isEmpty(managedIps)){
            log.warn("Ip pool {} does not have any available ip address.", ipPool.getName());
            return Optional.empty();
        }

        ManagedIp managedIp = managedIps.get(0);

        try {
            managedIp.allocate(usageResource, null);
        } catch (AllocatingIpReachableException e){
            managedIp.markExcluded(e.getMessage());
            ipRepository.saveWithoutTransaction(managedIp);
            throw e;
        }

        managedIp = ipRepository.save(managedIp);
        return Optional.of(managedIp);
    }
}
