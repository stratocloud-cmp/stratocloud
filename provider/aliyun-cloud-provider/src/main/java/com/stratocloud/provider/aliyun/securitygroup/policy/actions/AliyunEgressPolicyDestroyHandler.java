package com.stratocloud.provider.aliyun.securitygroup.policy.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunEgressPolicyHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicy;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEgressPolicyDestroyHandler implements DestroyResourceActionHandler {
    private final AliyunEgressPolicyHandler policyHandler;

    public AliyunEgressPolicyDestroyHandler(AliyunEgressPolicyHandler policyHandler) {
        this.policyHandler = policyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "删除出站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunSecurityGroupPolicy> policy = policyHandler.describeEgressPolicy(
                account, resource.getExternalId()
        );
        if(policy.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) policyHandler.getProvider();

        provider.buildClient(account).ecs().deleteEgressPolicy(policy.get().policyId());
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(AliyunEgressPolicyHandler.SOURCE_GROUP_REL_TYPE);
    }
}
