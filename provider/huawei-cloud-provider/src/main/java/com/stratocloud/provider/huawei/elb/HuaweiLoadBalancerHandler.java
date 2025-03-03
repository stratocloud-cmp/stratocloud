package com.stratocloud.provider.huawei.elb;

import com.huaweicloud.sdk.elb.v3.model.ListLoadBalancersRequest;
import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiLoadBalancerHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    private final IpAllocator ipAllocator;

    public HuaweiLoadBalancerHandler(HuaweiCloudProvider provider, IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_LOAD_BALANCER";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云负载均衡器";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<LoadBalancer> loadBalancer = describeLoadBalancer(account, externalId);
        return loadBalancer.map(lb -> toExternalResource(account, lb));
    }

    public Optional<LoadBalancer> describeLoadBalancer(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).elb().describeLoadBalancer(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, LoadBalancer lb) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                lb.getId(),
                lb.getName(),
                convertState(lb)
        );
    }

    private ResourceState convertState(LoadBalancer lb) {
        if(Objects.equals(lb.getOperatingStatus(), "FROZEN"))
            return ResourceState.UNAVAILABLE;

        return switch (lb.getProvisioningStatus()){
            case "PENDING_CREATE" -> ResourceState.BUILDING;
            case "ACTIVE" -> ResourceState.STARTED;
            case "PENDING_DELETE" -> ResourceState.DESTROYING;
            case "DELETED" -> ResourceState.DESTROYED;
            case "PENDING_UPDATE" -> ResourceState.CONFIGURING;
            case "ERROR" -> ResourceState.ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }


    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {

        return provider.buildClient(account).elb().describeLoadBalancers(
                new ListLoadBalancersRequest()
        ).stream().map(
                lb -> toExternalResource(account, lb)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        LoadBalancer elb = describeLoadBalancer(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB not found.")
        );

        resource.updateByExternal(toExternalResource(account, elb));

        String vipAddress = elb.getVipAddress();

        if(Utils.isNotBlank(vipAddress)){
            RuntimeProperty vipProperty = RuntimeProperty.ofDisplayInList(
                    "vip_address", "IP地址", vipAddress, vipAddress
            );

            resource.addOrUpdateRuntimeProperty(vipProperty);

            Optional<Resource> subnet = resource.getEssentialTarget(ResourceCategories.SUBNET);

            subnet.ifPresent(
                    s -> ipAllocator.forceAllocateIps(s, InternetProtocol.IPv4, List.of(vipAddress), resource)
            );
        }

        try {
            HuaweiLbStatusTreeHelper.synchronizeLbStatusTree(resource);
        }catch (Exception e){
            log.warn("Failed to synchronize status tree: {}.", e.toString());
        }
    }



    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }

    @Override
    public Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index) {
        return ResourcePropertiesUtil.getPropertiesAtIndex(properties, index, List.of("ips"));
    }
}
