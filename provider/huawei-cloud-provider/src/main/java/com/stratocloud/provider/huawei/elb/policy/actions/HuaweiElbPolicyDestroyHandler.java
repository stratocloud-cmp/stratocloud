package com.stratocloud.provider.huawei.elb.policy.actions;

import com.huaweicloud.sdk.elb.v3.model.L7Policy;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.policy.HuaweiElbPolicyHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbPolicyDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiElbPolicyHandler policyHandler;

    public HuaweiElbPolicyDestroyHandler(HuaweiElbPolicyHandler policyHandler) {
        this.policyHandler = policyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁转发策略";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) policyHandler.getProvider();

        Optional<L7Policy> l7Policy = policyHandler.describePolicy(account, resource.getExternalId());

        if(l7Policy.isEmpty())
            return;

        provider.buildClient(account).elb().deletePolicy(l7Policy.get().getId());
    }
}
