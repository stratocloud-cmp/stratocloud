package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackend;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEniBackendHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEniBackendToEniHandler implements EssentialRequirementHandler {

    private final AliyunClbEniBackendHandler backendHandler;

    private final AliyunNicHandler nicHandler;

    public AliyunEniBackendToEniHandler(AliyunClbEniBackendHandler backendHandler,
                                        AliyunNicHandler nicHandler) {
        this.backendHandler = backendHandler;
        this.nicHandler = nicHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ENI_BACKEND_TO_ENI_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云ENI后端服务与弹性网卡";
    }

    @Override
    public ResourceHandler getSource() {
        return backendHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return nicHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务";
    }

    @Override
    public String getRequirementName() {
        return "弹性网卡";
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

        Optional<ExternalResource> nic = nicHandler.describeExternalResource(account, serverId);

        if(nic.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        nic.get(),
                        Map.of()
                )
        );
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
