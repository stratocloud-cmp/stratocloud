package com.stratocloud.provider.tencent.lb.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.TencentLoadBalancerHandler;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;

import java.util.Map;
import java.util.Optional;

public abstract class TencentLoadBalancerDestroyHandler implements DestroyResourceActionHandler {

    protected final TencentLoadBalancerHandler loadBalancerHandler;

    protected TencentLoadBalancerDestroyHandler(TencentLoadBalancerHandler loadBalancerHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return loadBalancerHandler;
    }

    @Override
    public String getTaskName() {
        return "删除负载均衡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<LoadBalancer> loadBalancer
                = loadBalancerHandler.describeLoadBalancer(account, resource.getExternalId());

        if(loadBalancer.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) loadBalancerHandler.getProvider();

        provider.buildClient(account).deleteLoadBalancer(loadBalancer.get().getLoadBalancerId());
    }
}
