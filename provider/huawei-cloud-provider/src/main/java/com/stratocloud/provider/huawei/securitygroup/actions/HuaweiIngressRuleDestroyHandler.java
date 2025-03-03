package com.stratocloud.provider.huawei.securitygroup.actions;

import com.huaweicloud.sdk.vpc.v2.model.SecurityGroupRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.securitygroup.HuaweiIngressRuleHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiIngressRuleDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiIngressRuleHandler ruleHandler;

    public HuaweiIngressRuleDestroyHandler(HuaweiIngressRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "删除入站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<SecurityGroupRule> rule = ruleHandler.describeSecurityGroupRule(account, resource.getExternalId());

        if(rule.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) ruleHandler.getProvider();

        provider.buildClient(account).vpc().deleteSecurityGroupRule(rule.get().getId());
    }
}
