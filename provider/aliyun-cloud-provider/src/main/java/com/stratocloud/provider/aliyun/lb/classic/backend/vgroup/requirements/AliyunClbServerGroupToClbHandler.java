package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.lb.classic.AliyunClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunClbServerGroupToClbHandler implements ExclusiveRequirementHandler {

    protected final AliyunClbServerGroupHandler serverGroupHandler;

    protected final AliyunClbHandler clbHandler;

    protected AliyunClbServerGroupToClbHandler(AliyunClbServerGroupHandler serverGroupHandler,
                                               AliyunClbHandler clbHandler) {
        this.serverGroupHandler = serverGroupHandler;
        this.clbHandler = clbHandler;
    }

    @Override
    public String getRelationshipTypeName() {
        return "虚拟服务器组与CLB";
    }

    @Override
    public ResourceHandler getSource() {
        return serverGroupHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return clbHandler;
    }

    @Override
    public String getCapabilityName() {
        return "虚拟服务器组";
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
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunClbServerGroup> serverGroup = serverGroupHandler.describeServerGroup(
                account, source.externalId()
        );

        if(serverGroup.isEmpty())
            return List.of();

        Optional<ExternalResource> clb = clbHandler.describeExternalResource(
                account, serverGroup.get().id().loadBalancerId()
        );

        return clb.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
