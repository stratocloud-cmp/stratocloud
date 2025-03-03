package com.stratocloud.provider.tencent.securitygroup.policy.actions;

import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentPolicyService;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentSecurityGroupEgressPolicyHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.requirements.TencentEgressPolicyToSecurityGroupHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentEgressPolicyBuildHandler implements BuildResourceActionHandler {
    private final TencentSecurityGroupEgressPolicyHandler policyHandler;

    private final TencentPolicyService policyService;

    public TencentEgressPolicyBuildHandler(TencentSecurityGroupEgressPolicyHandler policyHandler,
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
        return "创建出站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentEgressPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        policyService.createPolicy(resource, SecurityGroupPolicyDirection.egress);
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
                TencentEgressPolicyToSecurityGroupHandler.TYPE_ID
        );
    }
}
