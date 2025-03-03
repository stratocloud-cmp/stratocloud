package com.stratocloud.provider.huawei.servers.requirements;

import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.flavor.HuaweiFlavorHandler;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiServerToFlavorHandler implements ChangeableEssentialHandler {

    private final HuaweiServerHandler serverHandler;

    private final HuaweiFlavorHandler flavorHandler;

    public HuaweiServerToFlavorHandler(HuaweiServerHandler serverHandler,
                                       HuaweiFlavorHandler flavorHandler) {
        this.serverHandler = serverHandler;
        this.flavorHandler = flavorHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SERVER_TO_FLAVOR_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与规格";
    }

    @Override
    public ResourceHandler getSource() {
        return serverHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return flavorHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机";
    }

    @Override
    public String getRequirementName() {
        return "云主机规格";
    }

    @Override
    public String getConnectActionName() {
        return "应用规格";
    }

    @Override
    public String getDisconnectActionName() {
        return "弃用规格";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource serverResource = relationship.getSource();
        Resource flavorResource = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(serverResource.getAccountId());
        ServerDetail server = serverHandler.describeServer(account, serverResource.getExternalId()).orElseThrow(
                () -> new StratoException("Server not found when resizing.")
        );

        if(Objects.equals(server.getFlavor().getId(), flavorResource.getExternalId())){
            log.warn("Server {} is already using flavor {}.",
                    server.getName(), flavorResource.getName());
            return;
        }

        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).ecs().resizeServer(server.getId(), flavorResource.getExternalId());
    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        String serverId = relationship.getSource().getExternalId();
        var server = serverHandler.describeServer(account, serverId);

        if(server.isEmpty())
            return RelationshipActionResult.failed("Server not found.");

        if(Objects.equals(server.get().getStatus(), "RESIZE"))
            return RelationshipActionResult.inProgress();

        return ChangeableEssentialHandler.super.checkConnectResult(account, relationship);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<ServerDetail> server = serverHandler.describeServer(account, source.externalId());

        if(server.isEmpty())
            return List.of();

        Optional<ExternalResource> flavor
                = flavorHandler.describeExternalResource(account, server.get().getFlavor().getId());

        return flavor.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
