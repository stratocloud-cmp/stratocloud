package com.stratocloud.provider.aliyun.lb.classic;

import com.aliyun.slb20140515.models.DescribeLoadBalancersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunClbHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunClbHandler(AliyunCloudProvider provider) {
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
    public boolean isSharedRequirementTarget() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<AliyunClb> clb = describeClb(account, externalId);

        return clb.map(
                value -> toExternalResource(account, value)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunClb clb) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                clb.detail().getLoadBalancerId(),
                clb.detail().getLoadBalancerName(),
                convertStatus(clb.detail().getLoadBalancerStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status) {
            case "inactive" -> ResourceState.STOPPED;
            case "active" -> ResourceState.STARTED;
            case "locked" -> ResourceState.UNAVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<AliyunClb> describeClb(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).clb().describeLoadBalancer(externalId).filter(this::filterLb);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();

        return provider.buildClient(account).clb().describeLoadBalancers(request).stream().filter(
                this::filterLb
        ).map(
                clb -> toExternalResource(account, clb)
        ).toList();
    }

    protected abstract boolean filterLb(AliyunClb aliyunClb);

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClb clb = describeClb(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB not found.")
        );

        resource.updateByExternal(toExternalResource(account, clb));

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type", "网络类型", clb.detail().getAddressType(), clb.detail().getAddressType()
        );

        RuntimeProperty specProperty = RuntimeProperty.ofDisplayable(
                "spec", "性能规格", clb.detail().getLoadBalancerSpec(), clb.detail().getLoadBalancerSpec()
        );

        resource.addOrUpdateRuntimeProperty(typeProperty);
        resource.addOrUpdateRuntimeProperty(specProperty);

        String address = clb.detail().getAddress();
        if(Utils.isNotBlank(address)){
            RuntimeProperty addressProperty = RuntimeProperty.ofDisplayInList(
                    "address", "IP地址", address, address
            );
            resource.addOrUpdateRuntimeProperty(addressProperty);
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
