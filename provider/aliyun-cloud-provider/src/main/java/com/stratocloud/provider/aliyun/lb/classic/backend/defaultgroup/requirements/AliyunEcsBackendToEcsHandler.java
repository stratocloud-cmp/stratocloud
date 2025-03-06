package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackend;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEcsBackendHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEcsBackendToEcsHandler implements EssentialRequirementHandler {

    private final AliyunClbEcsBackendHandler backendHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunEcsBackendToEcsHandler(AliyunClbEcsBackendHandler backendHandler,
                                        AliyunInstanceHandler instanceHandler) {
        this.backendHandler = backendHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ECS_BACKEND_TO_ECS_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云ECS后端服务与云主机";
    }

    @Override
    public ResourceHandler getSource() {
        return backendHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务";
    }

    @Override
    public String getRequirementName() {
        return "云主机";
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
        Optional<AliyunClbBackend> backend = backendHandler.describeBackend(account, source.externalId());

        if(backend.isEmpty())
            return List.of();

        String serverId = backend.get().detail().getServerId();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(account, serverId);

        return instance.map(externalResource -> List.of(
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
