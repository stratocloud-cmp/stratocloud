package com.stratocloud.provider.tencent.lb.rule.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL7ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.rule.TencentL7ListenerRuleHandler;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentRuleToL7ListenerHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_RULE_TO_L7_LISTENER_RELATIONSHIP";
    private final TencentL7ListenerRuleHandler ruleHandler;

    private final TencentL7ListenerHandler listenerHandler;

    public TencentRuleToL7ListenerHandler(TencentL7ListenerRuleHandler ruleHandler,
                                          TencentL7ListenerHandler listenerHandler) {
        this.ruleHandler = ruleHandler;
        this.listenerHandler = listenerHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云七层监听器与转发规则";
    }

    @Override
    public ResourceHandler getSource() {
        return ruleHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return listenerHandler;
    }

    @Override
    public String getCapabilityName() {
        return "转发规则";
    }

    @Override
    public String getRequirementName() {
        return "监听器";
    }

    @Override
    public String getConnectActionName() {
        return "添加规则";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除规则";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        TencentL7RuleId ruleId = TencentL7RuleId.fromString(source.externalId());
        TencentListenerId listenerId = new TencentListenerId(ruleId.lbId(), ruleId.listenerId());
        Optional<ExternalResource> listener = listenerHandler.describeExternalResource(account, listenerId.toString());
        if(listener.isEmpty())
            return List.of();
        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        listener.get(),
                        Map.of()
                )
        );
    }
}
