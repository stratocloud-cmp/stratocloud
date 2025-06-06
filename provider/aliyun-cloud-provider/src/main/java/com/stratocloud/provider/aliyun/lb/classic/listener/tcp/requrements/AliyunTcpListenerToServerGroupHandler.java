package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.requrements;

import com.aliyun.slb20140515.models.SetLoadBalancerTCPListenerAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.AliyunTcpListenerHandler;
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
public class AliyunTcpListenerToServerGroupHandler implements ExclusiveRequirementHandler {

    private final AliyunTcpListenerHandler listenerHandler;

    private final AliyunClbServerGroupHandler serverGroupHandler;

    public AliyunTcpListenerToServerGroupHandler(AliyunTcpListenerHandler listenerHandler,
                                                 AliyunClbServerGroupHandler serverGroupHandler) {
        this.listenerHandler = listenerHandler;
        this.serverGroupHandler = serverGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_TCP_LISTENER_TO_SERVER_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "监听器与虚拟服务器组";
    }

    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return serverGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "TCP监听器";
    }

    @Override
    public String getRequirementName() {
        return "虚拟服务器组";
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
    public void connect(Relationship relationship) {
        Resource listener = relationship.getSource();
        Resource group = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(group.getAccountId());

        AliyunListener aliyunListener = listenerHandler.describeListener(account, listener.getExternalId()).orElseThrow(
                () -> new StratoException("Listener not found when connecting to server group.")
        );

        AliyunClbServerGroup serverGroup = serverGroupHandler.describeServerGroup(account, group.getExternalId()).orElseThrow(
                () -> new StratoException("Server group not found when getting connected by listener.")
        );

        var request = new SetLoadBalancerTCPListenerAttributeRequest();
        request.setLoadBalancerId(aliyunListener.listenerId().loadBalancerId());
        request.setListenerPort(Integer.valueOf(aliyunListener.listenerId().port()));

        request.setVServerGroup("on");
        request.setVServerGroupId(serverGroup.id().serverGroupId());

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setTcpListenerAttributes(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource listener = relationship.getSource();
        Resource group = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(group.getAccountId());

        Optional<AliyunListener> aliyunListener = listenerHandler.describeListener(account, listener.getExternalId());

        if(aliyunListener.isEmpty())
            return;

        Optional<AliyunClbServerGroup> serverGroup
                = serverGroupHandler.describeServerGroup(account, group.getExternalId());

        if(serverGroup.isEmpty())
            return;

        var request = new SetLoadBalancerTCPListenerAttributeRequest();
        request.setLoadBalancerId(aliyunListener.get().listenerId().loadBalancerId());
        request.setListenerPort(Integer.valueOf(aliyunListener.get().listenerId().port()));

        request.setVServerGroup("off");

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setTcpListenerAttributes(request);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunListener> aliyunListener = listenerHandler.describeListener(account, source.externalId());

        if(aliyunListener.isEmpty())
            return List.of();

        String vServerGroupId = aliyunListener.get().detail().getVServerGroupId();

        if(Utils.isBlank(vServerGroupId))
            return List.of();

        AliyunClbServerGroupId serverGroupId = new AliyunClbServerGroupId(
                aliyunListener.get().listenerId().loadBalancerId(),
                vServerGroupId
        );

        Optional<ExternalResource> serverGroup
                = serverGroupHandler.describeExternalResource(account, serverGroupId.toString());

        return serverGroup.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
