package com.stratocloud.provider.aliyun.instance.requirements;

import com.aliyun.ecs20140526.models.AttachKeyPairRequest;
import com.aliyun.ecs20140526.models.DetachKeyPairRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.keypair.AliyunKeyPairHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunInstanceToKeyPairHandler implements ExclusiveRequirementHandler {

    private final AliyunInstanceHandler instanceHandler;

    private final AliyunKeyPairHandler keyPairHandler;

    public AliyunInstanceToKeyPairHandler(AliyunInstanceHandler instanceHandler,
                                          AliyunKeyPairHandler keyPairHandler) {
        this.instanceHandler = instanceHandler;
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_INSTANCE_TO_KEY_PAIR_RELATIONSHIP";
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
        AliyunCloudProvider provider = (AliyunCloudProvider) keyPairHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        AttachKeyPairRequest request = new AttachKeyPairRequest();
        request.setInstanceIds(JSON.toJsonString(List.of(instance.getExternalId())));
        request.setKeyPairName(keyPair.getName());

        client.ecs().attachKeyPair(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource keyPair = relationship.getTarget();
        Resource instance = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(keyPair.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) keyPairHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        DetachKeyPairRequest request = new DetachKeyPairRequest();
        request.setInstanceIds(JSON.toJsonString(List.of(instance.getExternalId())));
        request.setKeyPairName(keyPair.getName());

        client.ecs().detachKeyPair(request);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        String keyPairName = instance.get().detail().getKeyPairName();

        if(Utils.isBlank(keyPairName))
            return result;

        Optional<ExternalResource> keyPair = keyPairHandler.describeExternalResource(account, keyPairName);

        if(keyPair.isEmpty())
            return result;

        result.add(new ExternalRequirement(
                getRelationshipTypeId(),
                keyPair.get(),
                Map.of()
        ));

        return result;
    }
}
