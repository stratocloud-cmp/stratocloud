package com.stratocloud.ip;
import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.ip.query.DescribeIpsRequest;
import com.stratocloud.ip.query.NestedIpResponse;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Pageable;

import com.stratocloud.ip.cmd.*;
import com.stratocloud.ip.query.DescribeIpPoolRequest;
import com.stratocloud.ip.query.NestedIpPoolResponse;
import com.stratocloud.ip.response.*;
import com.stratocloud.repository.IpPoolRepository;
import com.stratocloud.repository.ManagedIpRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IpPoolServiceImpl implements IpPoolService {

    private final IpPoolRepository ipPoolRepository;

    private final ManagedIpRepository ipRepository;

    private final IpPoolAssembler assembler;

    private final ResourceRepository resourceRepository;

    public IpPoolServiceImpl(IpPoolRepository ipPoolRepository,
                             ManagedIpRepository ipRepository,
                             IpPoolAssembler assembler,
                             ResourceRepository resourceRepository) {
        this.ipPoolRepository = ipPoolRepository;
        this.ipRepository = ipRepository;
        this.assembler = assembler;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateIpPoolResponse createIpPool(CreateIpPoolCmd cmd) {
        Long tenantId = cmd.getTenantId();
        String name = cmd.getName();
        String description = cmd.getDescription();
        InternetProtocol protocol = cmd.getProtocol();
        String cidr = cmd.getCidr();
        String gateway = cmd.getGateway();

        IpPool ipPool = new IpPool(name, description, protocol, new Cidr(cidr), new IpAddress(gateway));
        ipPool.setTenantId(tenantId);

        ipPool = ipPoolRepository.save(ipPool);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        return new CreateIpPoolResponse(ipPool.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateIpPoolResponse updateIpPool(UpdateIpPoolCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        String name = cmd.getName();
        String description = cmd.getDescription();
        String cidr = cmd.getCidr();
        String gateway = cmd.getGateway();

        IpPool ipPool = ipPoolRepository.findIpPool(ipPoolId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        ipPool.update(name, description, new Cidr(cidr), new IpAddress(gateway));

        ipPoolRepository.save(ipPool);

        return new UpdateIpPoolResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteIpPoolsResponse deleteIpPools(DeleteIpPoolsCmd cmd) {
        List<Long> ipPoolIds = cmd.getIpPoolIds();
        ipPoolIds.forEach(this::deleteIpPool);
        return new DeleteIpPoolsResponse();
    }

    private void deleteIpPool(Long ipPoolId) {
        IpPool ipPool = ipPoolRepository.findIpPool(ipPoolId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        ipPool.preDeleteCheck();
        ipPoolRepository.delete(ipPool);
    }

    @Override
    @Transactional
    @ValidateRequest
    public AddIpRangeResponse addIpRange(AddIpRangeCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        String startIp = cmd.getStartIp();
        String endIp = cmd.getEndIp();

        IpPool ipPool = ipPoolRepository.findIpPool(ipPoolId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        ipPool.addRange(new IpAddress(startIp), new IpAddress(endIp));
        ipPoolRepository.save(ipPool);
        return new AddIpRangeResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RemoveIpRangeResponse removeIpRanges(RemoveIpRangesCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        List<Long> ipRangeIds = cmd.getIpRangeIds();

        IpPool ipPool = ipPoolRepository.findIpPool(ipPoolId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        ipRangeIds.forEach(ipPool::removeRangeById);
        ipPoolRepository.save(ipPool);
        return new RemoveIpRangeResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public ExcludeIpsResponse excludeIps(ExcludeIpsCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        List<String> addresses = cmd.getAddresses();
        String reason = cmd.getReason();

        List<ManagedIp> managedIps = ipRepository.findManagedIps(ipPoolId, addresses);

        managedIps.forEach(managedIp -> {
            AuditLogContext.current().addAuditObject(
                    new AuditObject(managedIp.getId().toString(), managedIp.getAddress().address())
            );
            managedIp.markExcluded(reason);
        });

        ipRepository.saveAll(managedIps);

        return new ExcludeIpsResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public ReleaseIpsResponse releaseIps(ReleaseIpsCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        List<String> addresses = cmd.getAddresses();

        List<ManagedIp> managedIps = ipRepository.findManagedIps(ipPoolId, addresses);

        managedIps.forEach(managedIp -> {
            AuditLogContext.current().addAuditObject(
                    new AuditObject(managedIp.getId().toString(), managedIp.getAddress().address())
            );
            managedIp.release();
        });

        ipRepository.saveAll(managedIps);

        return new ReleaseIpsResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateAttachedNetworksResponse updateAttachedNetworks(UpdateAttachedNetworksCmd cmd) {
        Long ipPoolId = cmd.getIpPoolId();
        List<Long> networkIds = cmd.getAttachedNetworkResourceIds();

        IpPool ipPool = ipPoolRepository.findIpPool(ipPoolId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(ipPool.getId().toString(), ipPool.getName())
        );

        List<Resource> networks = resourceRepository.findAllById(networkIds);

        ipPool.updateAttachedNetworkResources(networks);

        ipPoolRepository.save(ipPool);

        return new UpdateAttachedNetworksResponse();
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedIpPoolResponse> describeIpPools(DescribeIpPoolRequest request) {
        List<Long> ipPoolIds = request.getIpPoolIds();
        List<Long> networkResourceIds = request.getNetworkResourceIds();
        String search = request.getSearch();
        InternetProtocol protocol = request.getProtocol();
        Pageable pageable = request.getPageable();

        IpPoolFilters filters = new IpPoolFilters(ipPoolIds, networkResourceIds, search, protocol);

        Page<IpPool> page = ipPoolRepository.page(filters, pageable);

        return page.map(assembler::toNestedIpPoolResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedIpResponse> describeIps(DescribeIpsRequest request) {
        Long ipPoolId = request.getIpPoolId();
        Long networkResourceId = request.getNetworkResourceId();
        InternetProtocol protocol = request.getProtocol();

        List<String> ips = request.getIps();

        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<ManagedIp> page = ipRepository.page(ipPoolId, networkResourceId, protocol, ips, search, pageable);

        return page.map(assembler::toNestedIpResponse);
    }
}
