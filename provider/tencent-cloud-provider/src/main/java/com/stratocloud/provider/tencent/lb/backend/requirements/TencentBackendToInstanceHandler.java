package com.stratocloud.provider.tencent.lb.backend.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.lb.backend.TencentBackend;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentBackendToInstanceHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_BACKEND_TO_INSTANCE_RELATIONSHIP";
    private final TencentInstanceBackendHandler backendHandler;

    private final TencentInstanceHandler instanceHandler;

    public TencentBackendToInstanceHandler(TencentInstanceBackendHandler backendHandler,
                                           TencentInstanceHandler instanceHandler) {
        this.backendHandler = backendHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡后端与云主机";
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
        return "负载均衡后端";
    }

    @Override
    public String getRequirementName() {
        return "云主机";
    }

    @Override
    public String getConnectActionName() {
        return "绑定云主机";
    }

    @Override
    public String getDisconnectActionName() {
        return "解绑云主机";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<TencentBackend> backend = backendHandler.describeBackend(account, source.externalId());

        if(backend.isEmpty())
            return List.of();

        String instanceId = backend.get().backend().getInstanceId();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(account, instanceId);

        return instance.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
