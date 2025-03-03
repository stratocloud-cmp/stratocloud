package com.stratocloud.provider.tencent.securitygroup.policy.actions;

import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentPolicyService;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentSecurityGroupIngressPolicyHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.requirements.TencentIngressPolicyToSecurityGroupHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentIngressPolicyBuildHandler implements BuildResourceActionHandler {
    private final TencentSecurityGroupIngressPolicyHandler policyHandler;
    private final TencentPolicyService policyService;

    public TencentIngressPolicyBuildHandler(TencentSecurityGroupIngressPolicyHandler policyHandler,
                                            TencentPolicyService policyService) {
        this.policyHandler = policyHandler;
        this.policyService = policyService;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "创建入站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentIngressPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        policyService.createPolicy(resource, SecurityGroupPolicyDirection.ingress);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentIngressPolicyToSecurityGroupHandler.TYPE_ID
        );
    }
}
