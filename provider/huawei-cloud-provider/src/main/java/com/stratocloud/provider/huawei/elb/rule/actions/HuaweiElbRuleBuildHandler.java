package com.stratocloud.provider.huawei.elb.rule.actions;

import com.huaweicloud.sdk.elb.v3.model.CreateL7RuleRequest;
import com.huaweicloud.sdk.elb.v3.model.CreateL7RuleRequestBody;
import com.huaweicloud.sdk.elb.v3.model.CreateRuleCondition;
import com.huaweicloud.sdk.elb.v3.model.CreateRuleOption;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.rule.HuaweiElbRuleHandler;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRuleId;
import com.stratocloud.provider.huawei.elb.rule.requirements.HuaweiElbRuleToPolicyHandler;
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

@Component
public class HuaweiElbRuleBuildHandler implements BuildResourceActionHandler {

    private final HuaweiElbRuleHandler ruleHandler;

    public HuaweiElbRuleBuildHandler(HuaweiElbRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "创建转发规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiElbRuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiElbRuleBuildInput input = JSON.convert(parameters, HuaweiElbRuleBuildInput.class);

        CreateRuleOption option = new CreateRuleOption();

        String compareType;
        String key;

        switch (input.getType()){
            case "PATH" -> {
                compareType = input.getCompareType();
                key = "";
            }
            case "HEADER", "QUERY_STRING", "COOKIE" -> {
                compareType = "EQUAL_TO";
                key = input.getKey();
            }
            default -> {
                compareType = "EQUAL_TO";
                key = "";
            }
        }

        option.withType(input.getType()).withCompareType(compareType);

        if(Utils.isNotEmpty(input.getValues())){
            for (String value : input.getValues()) {
                option.addConditionsItem(new CreateRuleCondition().withKey(key).withValue(value));
            }
        }

        var policy = resource.getEssentialTargetByType(HuaweiElbRuleToPolicyHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Policy not provided")
        );

        HuaweiCloudProvider provider = (HuaweiCloudProvider) ruleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiRuleId ruleId = provider.buildClient(account).elb().createRule(
                new CreateL7RuleRequest().withL7policyId(policy.getExternalId()).withBody(
                        new CreateL7RuleRequestBody().withRule(option)
                )
        );
        resource.setExternalId(ruleId.toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
