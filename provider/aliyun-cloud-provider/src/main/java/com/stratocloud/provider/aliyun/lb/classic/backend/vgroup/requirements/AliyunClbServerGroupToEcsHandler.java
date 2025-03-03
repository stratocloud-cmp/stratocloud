package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.aliyun.slb20140515.models.AddVServerGroupBackendServersRequest;
import com.aliyun.slb20140515.models.RemoveVServerGroupBackendServersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunClbServerGroupToEcsHandler implements RelationshipHandler {

    private final AliyunClbServerGroupHandler serverGroupHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunClbServerGroupToEcsHandler(AliyunClbServerGroupHandler serverGroupHandler,
                                            AliyunInstanceHandler instanceHandler) {
        this.serverGroupHandler = serverGroupHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_CLB_SERVER_GROUP_TO_ECS_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "虚拟服务器组与云主机";
    }

    @Override
    public ResourceHandler getSource() {
        return serverGroupHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "虚拟服务器组";
    }

    @Override
    public String getRequirementName() {
        return "云主机";
    }

    @Override
    public String getConnectActionName() {
        return "移入云主机";
    }

    @Override
    public String getDisconnectActionName() {
        return "移出云主机";
    }


    @Override
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return AliyunClbServerGroupConnectInput.class;
    }

    @Override
    public void connect(Relationship relationship) {
        AliyunClbServerGroupConnectInput input = JSON.convert(
                relationship.getProperties(), AliyunClbServerGroupConnectInput.class
        );

        Resource serverGroup = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AddVServerGroupBackendServersRequest request = new AddVServerGroupBackendServersRequest();
        request.setVServerGroupId(serverGroup.getExternalId());

        Map<String, Object> backendServer = new HashMap<>();

        String resourceType = "ecs";

        backendServer.put("ServerId", instance.getExternalId());
        backendServer.put("Weight", input.getWeight().toString());
        backendServer.put("Type", resourceType);
        backendServer.put("Port", input.getPort().toString());

        request.setBackendServers(JSON.toJsonString(List.of(backendServer)));

        provider.buildClient(account).clb().addServerGroupBackendServer(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        AliyunClbServerGroupConnectInput input = JSON.convert(
                relationship.getProperties(), AliyunClbServerGroupConnectInput.class
        );

        Resource serverGroup = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        RemoveVServerGroupBackendServersRequest request = new RemoveVServerGroupBackendServersRequest();
        request.setVServerGroupId(serverGroup.getExternalId());

        Map<String, Object> backendServer = new HashMap<>();

        String resourceType = "ecs";

        backendServer.put("ServerId", instance.getExternalId());
        backendServer.put("Weight", input.getWeight().toString());
        backendServer.put("Type", resourceType);
        backendServer.put("Port", input.getPort().toString());

        request.setBackendServers(JSON.toJsonString(List.of(backendServer)));

        provider.buildClient(account).clb().removeServerGroupBackendServer(request);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        List<ExternalRequirement> result = new ArrayList<>();

        Optional<AliyunClbServerGroup> serverGroup
                = serverGroupHandler.describeServerGroup(account, source.externalId());

        if(serverGroup.isEmpty())
            return result;

        var backendServers = serverGroup.get().attributes().getBackendServers().getBackendServer();

        if(Utils.isEmpty(backendServers))
            return result;

        for (var backendServer : backendServers) {
            if(Objects.equals(backendServer.getType(), "ecs")){
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
