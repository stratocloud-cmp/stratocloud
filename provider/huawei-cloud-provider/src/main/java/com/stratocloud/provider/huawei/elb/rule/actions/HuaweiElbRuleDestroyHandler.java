package com.stratocloud.provider.huawei.elb.rule.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.rule.HuaweiElbRuleHandler;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRule;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbRuleDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiElbRuleHandler ruleHandler;

    public HuaweiElbRuleDestroyHandler(HuaweiElbRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "删除转发规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) ruleHandler.getProvider();

        Optional<HuaweiRule> rule = ruleHandler.describeRule(account, resource.getExternalId());

        if(rule.isEmpty())
            return;

        provider.buildClient(account).elb().deleteRule(rule.get().id());
    }
}
