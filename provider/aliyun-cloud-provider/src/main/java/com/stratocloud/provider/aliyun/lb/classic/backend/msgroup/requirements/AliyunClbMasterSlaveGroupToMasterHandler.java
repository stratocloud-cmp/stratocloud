package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements.AliyunClbServerGroupConnectInput;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunClbMasterSlaveGroupToMasterHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "ALIYUN_CLB_MASTER_SLAVE_GROUP_TO_MASTER_RELATIONSHIP";
    private final AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunClbMasterSlaveGroupToMasterHandler(AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler,
                                                    AliyunInstanceHandler instanceHandler) {
        this.masterSlaveGroupHandler = masterSlaveGroupHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "主备服务器组与主服务器";
    }

    @Override
    public ResourceHandler getSource() {
        return masterSlaveGroupHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "主备服务器组(主)";
    }

    @Override
    public String getRequirementName() {
        return "主服务器";
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
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return AliyunClbServerGroupConnectInput.class;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        List<ExternalRequirement> result = new ArrayList<>();

        Optional<AliyunClbMasterSlaveGroup> masterSlaveGroup
                = masterSlaveGroupHandler.describeMasterSlaveGroup(account, source.externalId());

        if(masterSlaveGroup.isEmpty())
            return result;

        var backendServers
                = masterSlaveGroup.get().attributes().getMasterSlaveBackendServers().getMasterSlaveBackendServer();

        if(Utils.isEmpty(backendServers))
            return result;

        for (var backendServer : backendServers) {
            boolean isEcs = Objects.equals(backendServer.getType(), "ecs");
            boolean isMaster = Objects.equals(backendServer.getServerType(), "Master");

            if(isEcs && isMaster){
                Optional<ExternalResource> instance = instanceHandler.describeExternalResource(
                        account, backendServer.getServerId()
                );

                if(instance.isPresent()){
                    AliyunClbServerGroupConnectInput input = new AliyunClbServerGroupConnectInput();
                    input.setPort(backendServer.getPort());
                    input.setWeight(backendServer.getWeight());
                    result.add(new ExternalRequirement(
                            getRelationshipTypeId(),
                            instance.get(),
                            JSON.toMap(input)
                    ));
                }
            }
        }

        return result;
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
