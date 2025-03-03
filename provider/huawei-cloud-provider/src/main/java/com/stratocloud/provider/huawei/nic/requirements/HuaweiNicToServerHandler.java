package com.stratocloud.provider.huawei.nic.requirements;

import com.huaweicloud.sdk.ecs.v2.model.InterfaceAttachment;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.nic.HuaweiNicHelper;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiNicToServerHandler implements ExclusiveRequirementHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiServerHandler serverHandler;

    public HuaweiNicToServerHandler(HuaweiNicHandler nicHandler, HuaweiServerHandler serverHandler) {
        this.nicHandler = nicHandler;
        this.serverHandler = serverHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_NIC_TO_SERVER_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与网卡";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return serverHandler;
    }

    @Override
    public String getCapabilityName() {
        return "辅助网卡";
    }

    @Override
    public String getRequirementName() {
        return "云主机(辅助网卡)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource server = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();

        provider.buildClient(account).ecs().attachPort(server.getExternalId(), nic.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource server = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();

        provider.buildClient(account).ecs().detachPort(server.getExternalId(), nic.getExternalId());
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        Resource nicResource = relationship.getSource();
        Resource serverResource = relationship.getTarget();
        String portId = nicResource.getExternalId();

        if(Utils.isBlank(portId))
            return RelationshipActionResult.finished();

        Optional<ServerDetail> server = serverHandler.describeServer(account, serverResource.getExternalId());

        if(server.isEmpty())
            return RelationshipActionResult.finished();

        if(isOnlyPortOnServer(account, server.get(), portId)) {
            log.warn("Port {} is the only port on server {}, cannot detach.",
                    portId, server.get().getName());
            return RelationshipActionResult.finished();
        }

        return ExclusiveRequirementHandler.super.checkDisconnectResult(account, relationship);
    }

    private boolean isOnlyPortOnServer(ExternalAccount account, ServerDetail server, String portId) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        List<InterfaceAttachment> attachments
                = provider.buildClient(account).ecs().listServerInterfaces(server.getId());

        if(Utils.isEmpty(attachments))
            return false;

        return attachments.size() == 1 && Objects.equals(attachments.get(0).getPortId(), portId);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Port> port = nicHandler.describePort(account, source.externalId());

        if(port.isEmpty() || Utils.isBlank(port.get().getDeviceId()))
            return List.of();

        if(HuaweiNicHelper.isPrimaryInterface(port.get()))
            return List.of();

        String deviceId = port.get().getDeviceId();

        Optional<ExternalResource> server = serverHandler.describeExternalResource(account, deviceId);

        return server.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);
    }

    @Override
    public boolean requireTargetResourceTaskLock() {
        return true;
    }
}
