package com.stratocloud.provider.aliyun.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunNicToInstanceHandler implements ExclusiveRequirementHandler {

    private final AliyunNicHandler nicHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunNicToInstanceHandler(AliyunNicHandler nicHandler,
                                      AliyunInstanceHandler instanceHandler) {
        this.nicHandler = nicHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_NIC_TO_INSTANCE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "辅助网卡与云主机";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
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
        return "挂载辅助网卡";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        client.ecs().attachNic(instance.getExternalId(), nic.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        client.ecs().detachNic(instance.getExternalId(), nic.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunNic> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        if(nic.get().isPrimaryNic())
            return List.of();

        if(Utils.isBlank(nic.get().detail().getInstanceId()))
            return List.of();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(
                account, nic.get().detail().getInstanceId()
        );

        return instance.map(
                externalResource -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                externalResource,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }


    @Override
    public boolean requireTargetResourceTaskLock() {
        return true;
    }
}
