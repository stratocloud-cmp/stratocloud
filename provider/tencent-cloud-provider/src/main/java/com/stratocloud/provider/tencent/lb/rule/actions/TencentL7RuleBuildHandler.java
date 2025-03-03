package com.stratocloud.provider.tencent.lb.rule.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.rule.TencentL7ListenerRuleHandler;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.provider.tencent.lb.rule.requirements.TencentRuleToL7ListenerHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.CreateRuleRequest;
import com.tencentcloudapi.clb.v20180317.models.HealthCheck;
import com.tencentcloudapi.clb.v20180317.models.RuleInput;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentL7RuleBuildHandler implements BuildResourceActionHandler {

    private final TencentL7ListenerRuleHandler ruleHandler;

    public TencentL7RuleBuildHandler(TencentL7ListenerRuleHandler ruleHandler) {
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
        return TencentL7RuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource listener = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER).orElseThrow(
                () -> new StratoException("Listener not found when creating rule.")
        );

        TencentListenerId listenerId = TencentListenerId.fromString(listener.getExternalId());

        TencentL7RuleBuildInput input = JSON.convert(parameters, TencentL7RuleBuildInput.class);

        RuleInput ruleInput = new RuleInput();
        ruleInput.setUrl(input.getUrl());
        ruleInput.setDomains(input.getDomains().toArray(new String[0]));
        ruleInput.setSessionExpireTime(input.getSessionExpireTime());

        ruleInput.setScheduler(input.getScheduler());
        ruleInput.setForwardType(input.getForwardType());
        ruleInput.setDefaultServer(input.getDefaultServer());
        ruleInput.setHttp2(input.getEnableHttp2());
        ruleInput.setQuic(input.getEnableQuic());

        HealthCheck healthCheck = new HealthCheck();

        if(input.getEnableHealthCheck()!=null && input.getEnableHealthCheck()){
            healthCheck.setHealthSwitch(1L);
            healthCheck.setTimeOut(input.getHealthCheckTimeout());
            healthCheck.setIntervalTime(input.getHealthCheckInterval());
            healthCheck.setHealthNum(input.getHealthyNumber());
            healthCheck.setUnHealthNum(input.getUnhealthyNumber());
            healthCheck.setHttpCode(input.getHttpCode());
            if(Utils.isNotBlank(input.getHttpCheckPath()))
                healthCheck.setHttpCheckPath(input.getHttpCheckPath());
            if(Utils.isNotBlank(input.getHttpCheckDomain()))
                healthCheck.setHttpCheckDomain(input.getHttpCheckDomain());
            if(Utils.isNotBlank(input.getHttpCheckMethod()))
                healthCheck.setHttpCheckMethod(input.getHttpCheckMethod());
            healthCheck.setSourceIpType(input.getSourceIpType());
        }else {
            healthCheck.setHealthSwitch(0L);
        }
        ruleInput.setHealthCheck(healthCheck);


        CreateRuleRequest request = new CreateRuleRequest();
        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerId(listenerId.listenerId());
        request.setRules(new RuleInput[]{ruleInput});

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) ruleHandler.getProvider();
        String locationId = provider.buildClient(account).createRule(request);
        resource.setExternalId(
                new TencentL7RuleId(listenerId.lbId(), listenerId.listenerId(), locationId).toString()
        );
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
                TencentRuleToL7ListenerHandler.TYPE_ID
        );
    }
}
