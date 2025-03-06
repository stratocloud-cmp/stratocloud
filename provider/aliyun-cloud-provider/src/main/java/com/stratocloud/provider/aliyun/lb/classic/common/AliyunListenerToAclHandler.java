package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.lb.classic.acl.AliyunClbAclHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunListenerToAclHandler implements ExclusiveRequirementHandler {

    protected final AliyunListenerHandler listenerHandler;

    protected final AliyunClbAclHandler aclHandler;

    protected AliyunListenerToAclHandler(AliyunListenerHandler listenerHandler,
                                         AliyunClbAclHandler aclHandler) {
        this.listenerHandler = listenerHandler;
        this.aclHandler = aclHandler;
    }


    @Override
    public String getRelationshipTypeName() {
        return "监听器与ACL";
    }

    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return aclHandler;
    }

    @Override
    public String getRequirementName() {
        return "ACL";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunListener> listener = listenerHandler.describeListener(account, source.externalId());

        if(listener.isEmpty())
            return List.of();

        String aclId = listener.get().detail().getAclId();

        if(Utils.isBlank(aclId))
            return List.of();

        Optional<ExternalResource> acl = aclHandler.describeExternalResource(account, aclId);

        return acl.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
