package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.aliyun.slb20140515.models.AddVServerGroupBackendServersRequest;
import com.aliyun.slb20140515.models.RemoveVServerGroupBackendServersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
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
public class AliyunClbServerGroupToEniHandler implements RelationshipHandler {

    private final AliyunClbServerGroupHandler serverGroupHandler;

    private final AliyunNicHandler nicHandler;

    public AliyunClbServerGroupToEniHandler(AliyunClbServerGroupHandler serverGroupHandler,
                                            AliyunNicHandler nicHandler) {
        this.serverGroupHandler = serverGroupHandler;
        this.nicHandler = nicHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_CLB_SERVER_GROUP_TO_ENI_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "虚拟服务器组与弹性网卡";
    }

    @Override
    public ResourceHandler getSource() {
        return serverGroupHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return nicHandler;
    }

    @Override
    public String getCapabilityName() {
        return "虚拟服务器组";
    }

    @Override
    public String getRequirementName() {
        return "弹性网卡";
    }

    @Override
    public String getConnectActionName() {
        return "移入弹性网卡";
    }

    @Override
    public String getDisconnectActionName() {
        return "移出弹性网卡";
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
        Resource nic = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        AliyunNic aliyunNic = nicHandler.describeNic(account, nic.getExternalId()).orElseThrow(
                () -> new StratoException("Nic not found when adding to clb server group.")
        );

        AddVServerGroupBackendServersRequest request = new AddVServerGroupBackendServersRequest();
        request.setVServerGroupId(serverGroup.getExternalId());

        Map<String, Object> backendServer = new HashMap<>();

        String resourceType = "eni";

        backendServer.put("ServerId", nic.getExternalId());
        backendServer.put("ServerIp", aliyunNic.detail().getPrivateIpAddress());
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
        Resource nic = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        AliyunNic aliyunNic = nicHandler.describeNic(account, nic.getExternalId()).orElseThrow(
                () -> new StratoException("Nic not found when adding to clb server group.")
        );

        RemoveVServerGroupBackendServersRequest request = new RemoveVServerGroupBackendServersRequest();
        request.setVServerGroupId(serverGroup.getExternalId());

        Map<String, Object> backendServer = new HashMap<>();

        String resourceType = "eni";

        backendServer.put("ServerId", nic.getExternalId());
        backendServer.put("ServerIp", aliyunNic.detail().getPrivateIpAddress());
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
            if(Objects.equals(backendServer.getType(), "eni")){
                Optional<ExternalResource> nic = nicHandler.describeExternalResource(
                        account, backendServer.getServerId()
                );

                if(nic.isPresent()){
                    AliyunClbServerGroupConnectInput input = new AliyunClbServerGroupConnectInput();
                    input.setPort(backendServer.getPort());
                    input.setWeight(backendServer.getWeight());
                    result.add(new ExternalRequirement(
                            getRelationshipTypeId(),
                            nic.get(),
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
