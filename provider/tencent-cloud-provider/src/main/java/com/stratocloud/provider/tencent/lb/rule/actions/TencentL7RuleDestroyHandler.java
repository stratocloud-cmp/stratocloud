package com.stratocloud.provider.tencent.lb.rule.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.rule.TencentL7ListenerRuleHandler;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.provider.tencent.lb.rule.requirements.TencentRuleToL7ListenerHandler;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.clb.v20180317.models.DeleteRuleRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentL7RuleDestroyHandler implements DestroyResourceActionHandler {

    private final TencentL7ListenerRuleHandler ruleHandler;

    public TencentL7RuleDestroyHandler(TencentL7ListenerRuleHandler ruleHandler) {
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
        Optional<ExternalResource> rule = ruleHandler.describeExternalResource(account, resource.getExternalId());

        if(rule.isEmpty())
            return;

        TencentL7RuleId ruleId = TencentL7RuleId.fromString(resource.getExternalId());

        DeleteRuleRequest request = new DeleteRuleRequest();
        request.setLoadBalancerId(ruleId.lbId());
        request.setListenerId(ruleId.listenerId());
        request.setLocationIds(new String[]{ruleId.locationId()});

        TencentCloudProvider provider = (TencentCloudProvider) ruleHandler.getProvider();
        provider.buildClient(account).deleteRule(request);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentRuleToL7ListenerHandler.TYPE_ID
        );
    }
}
