package com.stratocloud.provider.aliyun.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunPrimaryNicToInstanceHandler
        implements ExclusiveRequirementHandler, EssentialPrimaryCapabilityHandler {

    private final AliyunNicHandler nicHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunPrimaryNicToInstanceHandler(AliyunNicHandler nicHandler,
                                             AliyunInstanceHandler instanceHandler) {
        this.nicHandler = nicHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_PRIMARY_NIC_TO_INSTANCE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "主网卡与云主机";
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
        return "主网卡";
    }

    @Override
    public String getRequirementName() {
        return "云主机(主网卡)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载主网卡";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunNic> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        if(!nic.get().isPrimaryNic())
            return List.of();

        String instanceId = nic.get().detail().getInstanceId();

        if(Utils.isBlank(instanceId))
            return List.of();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(
                account, instanceId
        );

        return instance.map(externalResource -> List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                externalResource,
                Map.of()
        ))).orElseGet(List::of);

    }
}
