package com.stratocloud.provider.huawei.servers.requirements;

import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.keypair.HuaweiKeyPairHandler;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiServerToKeyPairHandler implements ExclusiveRequirementHandler {

    private final HuaweiServerHandler serverHandler;

    private final HuaweiKeyPairHandler keyPairHandler;

    public HuaweiServerToKeyPairHandler(HuaweiServerHandler serverHandler,
                                        HuaweiKeyPairHandler keyPairHandler) {
        this.serverHandler = serverHandler;
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SERVER_TO_KEY_PAIR_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与密钥对";
    }

    @Override
    public ResourceHandler getSource() {
        return serverHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return keyPairHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机";
    }

    @Override
    public String getRequirementName() {
        return "密钥对";
    }

    @Override
    public String getConnectActionName() {
        return "绑定";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource server = relationship.getSource();
        Resource keyPair = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(server.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).kps().associateKeyPair(server.getExternalId(), keyPair.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource server = relationship.getSource();
        Resource keyPair = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(server.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).kps().disassociateKeyPair(server.getExternalId(), keyPair.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<ServerDetail> serverDetail = serverHandler.describeServer(account, source.externalId());

        if(serverDetail.isEmpty())
            return List.of();

        String keyName = serverDetail.get().getKeyName();

        Optional<ExternalResource> keyPair = keyPairHandler.describeExternalResource(account, keyName);

        return keyPair.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
