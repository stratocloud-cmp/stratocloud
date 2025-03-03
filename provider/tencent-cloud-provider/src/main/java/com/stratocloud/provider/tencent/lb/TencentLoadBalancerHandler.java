package com.stratocloud.provider.tencent.lb;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.DescribeLoadBalancersRequest;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class TencentLoadBalancerHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentLoadBalancerHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
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
        return loadBalancer.map(
                lb -> toExternalResource(account, lb)
        );
    }

    public Optional<LoadBalancer> describeLoadBalancer(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeLoadBalancer(externalId).filter(this::filterLb);
    }

    private ExternalResource toExternalResource(ExternalAccount account, LoadBalancer lb) {
        return new ExternalResource(
                account.getProviderId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                lb.getLoadBalancerId(),
                lb.getLoadBalancerName(),
                convertState(lb.getStatus())
        );
    }

    private ResourceState convertState(Long status) {
        if(Objects.equals(status, 0L))
            return ResourceState.BUILDING;
        else if(Objects.equals(status, 1L))
            return ResourceState.STARTED;
        else
            return ResourceState.UNKNOWN;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account,
                                                            Map<String, Object> queryArgs) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        List<LoadBalancer> loadBalancers = provider.buildClient(account).describeLoadBalancers(request);
        return loadBalancers.stream().filter(
                this::filterLb
        ).map(
                lb -> toExternalResource(account, lb)
        ).toList();
    }

    protected abstract boolean filterLb(LoadBalancer loadBalancer);

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        LoadBalancer loadBalancer = describeLoadBalancer(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB not found.")
        );

        resource.updateByExternal(toExternalResource(account, loadBalancer));

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type", "网络类型", loadBalancer.getLoadBalancerType(), loadBalancer.getLoadBalancerType()
        );

        RuntimeProperty slaTypeProperty = RuntimeProperty.ofDisplayable(
                "slaType", "规格", loadBalancer.getSlaType(), loadBalancer.getSlaType()
        );

        resource.addOrUpdateRuntimeProperty(typeProperty);
        resource.addOrUpdateRuntimeProperty(slaTypeProperty);

        if(Utils.isNotBlank(loadBalancer.getLoadBalancerDomain())){
            RuntimeProperty domainProperty = RuntimeProperty.ofDisplayInList(
                    "domain",
                    "域名",
                    loadBalancer.getLoadBalancerDomain(),
                    loadBalancer.getLoadBalancerDomain()
            );
            resource.addOrUpdateRuntimeProperty(domainProperty);
        }



        if(Utils.isNotEmpty(loadBalancer.getLoadBalancerVips())){
            String vips = String.join(",", loadBalancer.getLoadBalancerVips());
            RuntimeProperty vipProperty = RuntimeProperty.ofDisplayInList("vips", "VIP地址", vips, vips);
            resource.addOrUpdateRuntimeProperty(vipProperty);
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
}
