package com.stratocloud.provider.huawei.elb.policy.requirements;

import com.huaweicloud.sdk.elb.v3.model.L7Policy;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.listener.HuaweiListenerHandler;
import com.stratocloud.provider.huawei.elb.policy.HuaweiElbPolicyHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbPolicyToRedirectListenerHandler implements ExclusiveRequirementHandler {

    public static final String TYPE_ID = "HUAWEI_ELB_POLICY_TO_REDIRECT_LISTENER_RELATIONSHIP";
    private final HuaweiElbPolicyHandler policyHandler;

    private final HuaweiListenerHandler listenerHandler;

    public HuaweiElbPolicyToRedirectListenerHandler(HuaweiElbPolicyHandler policyHandler,
                                                    HuaweiListenerHandler listenerHandler) {
        this.policyHandler = policyHandler;
        this.listenerHandler = listenerHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "转发策略与重定向监听器";
    }

    @Override
    public ResourceHandler getSource() {
        return policyHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return listenerHandler;
    }

    @Override
    public String getCapabilityName() {
        return "重定向转发策略";
    }

    @Override
    public String getRequirementName() {
        return "重定向监听器";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<L7Policy> l7Policy = policyHandler.describePolicy(account, source.externalId());

        if(l7Policy.isEmpty())
            return List.of();

        var listener = listenerHandler.describeExternalResource(account, l7Policy.get().getRedirectListenerId());

        return listener.map(l -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        l,
                        Map.of()
                )
        )).orElseGet(List::of);
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
