package com.stratocloud.provider.tencent.securitygroup.policy.actions;

import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentPolicyService;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentSecurityGroupEgressPolicyHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.requirements.TencentEgressPolicyToSecurityGroupHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentEgressPolicyDestroyHandler implements DestroyResourceActionHandler {
    private final TencentSecurityGroupEgressPolicyHandler policyHandler;

    private final TencentPolicyService policyService;

    public TencentEgressPolicyDestroyHandler(TencentSecurityGroupEgressPolicyHandler policyHandler,
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
        return "删除出站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        policyService.removePolicy(resource);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        resource.onDestroyed();
        return ResourceActionResult.finished();
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentEgressPolicyToSecurityGroupHandler.TYPE_ID
        );
    }
}
