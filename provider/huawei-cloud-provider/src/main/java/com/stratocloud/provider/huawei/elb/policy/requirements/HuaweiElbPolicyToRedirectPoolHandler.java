package com.stratocloud.provider.huawei.elb.policy.requirements;

import com.huaweicloud.sdk.elb.v3.model.L7Policy;
import com.huaweicloud.sdk.elb.v3.model.RedirectPoolsConfig;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.NumberField;
import com.stratocloud.provider.huawei.elb.policy.HuaweiElbPolicyHandler;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbPolicyToRedirectPoolHandler implements RelationshipHandler {

    public static final String TYPE_ID = "HUAWEI_ELB_POLICY_TO_REDIRECT_POOL_RELATIONSHIP";
    private final HuaweiElbPolicyHandler policyHandler;

    private final HuaweiLbPoolHandler poolHandler;

    public HuaweiElbPolicyToRedirectPoolHandler(HuaweiElbPolicyHandler policyHandler,
                                                HuaweiLbPoolHandler poolHandler) {
        this.policyHandler = policyHandler;
        this.poolHandler = poolHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "转发策略与重定向服务器组";
    }

    @Override
    public ResourceHandler getSource() {
        return policyHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return poolHandler;
    }

    @Override
    public String getCapabilityName() {
        return "重定向转发策略";
    }

    @Override
    public String getRequirementName() {
        return "重定向服务器组";
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
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return PoolConfigInput.class;
    }

    @Data
    public static class PoolConfigInput implements RelationshipConnectInput {
        @NumberField(label = "转发权重", required = false)
        private Integer weight;
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

        List<ExternalRequirement> result = new ArrayList<>();

        List<RedirectPoolsConfig> poolsConfig = l7Policy.get().getRedirectPoolsConfig();

        if(Utils.isNotEmpty(poolsConfig)){
            for (RedirectPoolsConfig config : poolsConfig) {
                var pool = poolHandler.describeExternalResource(account, config.getPoolId());

                pool.ifPresent(p -> result.add(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                p,
                                Map.of()
                        )
                ));
            }
        }

        return result;
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
