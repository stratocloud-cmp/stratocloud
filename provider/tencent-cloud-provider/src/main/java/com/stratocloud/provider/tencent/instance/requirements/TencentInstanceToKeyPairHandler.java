package com.stratocloud.provider.tencent.instance.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.keypair.TencentKeyPairHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentInstanceToKeyPairHandler implements RelationshipHandler {

    private final TencentInstanceHandler instanceHandler;

    private final TencentKeyPairHandler keyPairHandler;

    public TencentInstanceToKeyPairHandler(TencentInstanceHandler instanceHandler,
                                           TencentKeyPairHandler keyPairHandler) {
        this.instanceHandler = instanceHandler;
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_INSTANCE_TO_KEY_PAIR_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与密钥对";
    }

    @Override
    public ResourceHandler getSource() {
        return instanceHandler;
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
        return "绑定密钥对";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public Set<ResourceState> getAllowedSourceStates() {
        return Set.of(ResourceState.STOPPED);
    }

    @Override
    public void connect(Relationship relationship) {
        Resource keyPair = relationship.getTarget();
        Resource instance = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(keyPair.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) keyPairHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        List<String> associatedKeyPairIds = instance.getRequirementTargets(ResourceCategories.KEY_PAIR).stream().map(
                Resource::getExternalId
        ).toList();

        List<String> keyPairIds = new ArrayList<>(associatedKeyPairIds);
        keyPairIds.add(keyPair.getExternalId());

        client.associateKeyPairs(instance.getExternalId(), keyPairIds);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource keyPair = relationship.getTarget();
        Resource instance = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(keyPair.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) keyPairHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        client.disassociateKeyPair(instance.getExternalId(), keyPair.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Instance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        if(instance.get().getLoginSettings() == null)
            return result;

        if(Utils.isEmpty(instance.get().getLoginSettings().getKeyIds()))
            return result;

        for (String keyId : instance.get().getLoginSettings().getKeyIds()) {
            Optional<ExternalResource> keyPair = keyPairHandler.describeExternalResource(account, keyId);

            if(keyPair.isEmpty())
                continue;

            ExternalRequirement keyPairRequirement = new ExternalRequirement(
                    getRelationshipTypeId(),
                    keyPair.get(),
                    Map.of()
            );

            result.add(keyPairRequirement);
        }

        return result;
    }
}
