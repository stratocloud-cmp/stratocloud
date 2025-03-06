package com.stratocloud.provider.tencent.lb.backend.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.backend.TencentBackend;
import com.stratocloud.provider.tencent.lb.backend.TencentNicBackendHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicBackendToL4ListenerHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_NIC_BACKEND_TO_L4_LISTENER_RELATIONSHIP";
    private final TencentNicBackendHandler backendHandler;

    private final TencentL4ListenerHandler listenerHandler;

    public TencentNicBackendToL4ListenerHandler(TencentNicBackendHandler backendHandler,
                                                TencentL4ListenerHandler listenerHandler) {
        this.backendHandler = backendHandler;
        this.listenerHandler = listenerHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "四层监听器与网卡后端服务";
    }

    @Override
    public ResourceHandler getSource() {
        return backendHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return listenerHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务(网卡)";
    }

    @Override
    public String getRequirementName() {
        return "监听器";
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

        Optional<ExternalResource> listener = listenerHandler.describeExternalResource(
                account,
                new TencentListenerId(backend.get().lbId(), backend.get().listenerId()).toString()
        );

        return listener.map(externalResource -> List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                externalResource,
                Map.of()
        ))).orElseGet(List::of);

    }
}
