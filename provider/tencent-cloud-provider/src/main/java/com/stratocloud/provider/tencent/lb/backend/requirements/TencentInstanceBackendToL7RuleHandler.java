package com.stratocloud.provider.tencent.lb.backend.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.backend.TencentBackend;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.rule.TencentL7ListenerRuleHandler;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.Backend;
import com.tencentcloudapi.clb.v20180317.models.ListenerBackend;
import com.tencentcloudapi.clb.v20180317.models.RuleTargets;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentInstanceBackendToL7RuleHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_INSTANCE_BACKEND_TO_L7_RULE_RELATIONSHIP";
    private final TencentInstanceBackendHandler backendHandler;

    private final TencentL7ListenerRuleHandler ruleHandler;

    public TencentInstanceBackendToL7RuleHandler(TencentInstanceBackendHandler backendHandler,
                                                 TencentL7ListenerRuleHandler ruleHandler) {
        this.backendHandler = backendHandler;
        this.ruleHandler = ruleHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "转发规则与云主机后端服务";
    }

    @Override
    public ResourceHandler getSource() {
        return backendHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return ruleHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务(云主机)";
    }

    @Override
    public String getRequirementName() {
        return "转发规则";
    }

    @Override
    public String getConnectActionName() {
        return "绑定后端服务";
    }

    @Override
    public String getDisconnectActionName() {
        return "解绑后端服务";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<TencentBackend> backend = backendHandler.describeBackend(account, source.externalId());

        if(backend.isEmpty()){
            return List.of();
        }

        TencentListenerId listenerId = new TencentListenerId(backend.get().lbId(), backend.get().listenerId());

        TencentCloudProvider provider = (TencentCloudProvider) ruleHandler.getProvider();

        Optional<ListenerBackend> listenerBackend = provider.buildClient(account).describeListenerBackend(listenerId);

        if(listenerBackend.isEmpty())
            return List.of();


        RuleTargets[] rules = listenerBackend.get().getRules();
        if(Utils.isEmpty(rules))
            return List.of();

        String locationId = null;
        for (RuleTargets rule : rules) {
            if(Utils.isEmpty(rule.getTargets()))
                continue;

            Optional<Backend> optionalBackend = Arrays.stream(rule.getTargets()).filter(
                    t -> Objects.equals(t.getInstanceId(), backend.get().backend().getInstanceId())
            ).findAny();

            if(optionalBackend.isPresent())
                locationId = rule.getLocationId();
        }

        if(Utils.isBlank(locationId))
            return List.of();

        String ruleId = new TencentL7RuleId(listenerId.lbId(), listenerId.listenerId(), locationId).toString();

        Optional<ExternalResource> rule = ruleHandler.describeExternalResource(account, ruleId);

        if(rule.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                rule.get(),
                Map.of()
        ));
    }
}
