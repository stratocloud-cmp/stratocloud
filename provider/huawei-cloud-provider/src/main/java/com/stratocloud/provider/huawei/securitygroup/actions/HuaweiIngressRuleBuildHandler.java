package com.stratocloud.provider.huawei.securitygroup.actions;

import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleOption;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleRequest;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.securitygroup.HuaweiIngressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.requirements.HuaweiIngressRuleToRemoteGroupHandler;
import com.stratocloud.provider.huawei.securitygroup.requirements.HuaweiIngressRuleToSecurityGroupHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiIngressRuleBuildHandler implements BuildResourceActionHandler {

    private final HuaweiIngressRuleHandler ruleHandler;

    public HuaweiIngressRuleBuildHandler(HuaweiIngressRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "添加入站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiIngressRuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource securityGroup = resource.getEssentialTargetByType(
                HuaweiIngressRuleToSecurityGroupHandler.TYPE_ID
        ).orElseThrow(() -> new StratoException("Security group not found when creating ingress rule."));

        Optional<Resource> remoteGroup = resource.getExclusiveTargetByType(
                HuaweiIngressRuleToRemoteGroupHandler.TYPE_ID
        );

        var input = JSON.convert(parameters, HuaweiIngressRuleBuildInput.class);

        CreateSecurityGroupRuleOption option = new CreateSecurityGroupRuleOption();
        option.setSecurityGroupId(securityGroup.getExternalId());
        option.setDescription(resource.getDescription());
        option.setDirection("ingress");
        option.setEthertype(input.getEtherType().name());

        if(!Objects.equals(input.getProtocol(), "any")) {
            option.setProtocol(input.getProtocol());
            option.setPortRangeMin(input.getPortMin());
            option.setPortRangeMax(input.getPortMax());
        }
        if(remoteGroup.isPresent())
            option.setRemoteGroupId(remoteGroup.get().getExternalId());
        else if(Utils.isNotBlank(input.getRemoteIpPrefix()))
            option.setRemoteIpPrefix(input.getRemoteIpPrefix());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) ruleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String ruleId = provider.buildClient(account).vpc().createSecurityGroupRule(
                new CreateSecurityGroupRuleRequest().withBody(
                        new CreateSecurityGroupRuleRequestBody().withSecurityGroupRule(option)
                )
        );
        resource.setExternalId(ruleId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
