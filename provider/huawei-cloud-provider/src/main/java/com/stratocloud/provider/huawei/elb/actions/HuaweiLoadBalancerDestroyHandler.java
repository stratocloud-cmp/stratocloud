package com.stratocloud.provider.huawei.elb.actions;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiLoadBalancerDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    public HuaweiLoadBalancerDestroyHandler(HuaweiLoadBalancerHandler loadBalancerHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return loadBalancerHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁负载均衡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<LoadBalancer> elb = loadBalancerHandler.describeLoadBalancer(account, resource.getExternalId());

        if(elb.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();

        provider.buildClient(account).elb().cascadeDeleteElb(elb.get().getId());
    }
}
