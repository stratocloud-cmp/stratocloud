package com.stratocloud.provider.huawei.securitygroup.actions;

import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleOption;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleRequest;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRuleRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.securitygroup.HuaweiEgressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.requirements.HuaweiEgressRuleToRemoteGroupHandler;
import com.stratocloud.provider.huawei.securitygroup.requirements.HuaweiEgressRuleToSecurityGroupHandler;
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
public class HuaweiEgressRuleBuildHandler implements BuildResourceActionHandler {

    private final HuaweiEgressRuleHandler ruleHandler;

    public HuaweiEgressRuleBuildHandler(HuaweiEgressRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "添加出站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiEgressRuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource securityGroup = resource.getEssentialTargetByType(
                HuaweiEgressRuleToSecurityGroupHandler.TYPE_ID
        ).orElseThrow(() -> new StratoException("Security group not found when creating egress rule."));

        Optional<Resource> remoteGroup = resource.getExclusiveTargetByType(
                HuaweiEgressRuleToRemoteGroupHandler.TYPE_ID
        );

        var input = JSON.convert(parameters, HuaweiEgressRuleBuildInput.class);

        CreateSecurityGroupRuleOption option = new CreateSecurityGroupRuleOption();
        option.setSecurityGroupId(securityGroup.getExternalId());
        option.setDescription(resource.getDescription());
        option.setDirection("egress");
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
