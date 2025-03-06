package com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements;

import com.aliyun.slb20140515.models.SetLoadBalancerUDPListenerAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
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
public class AliyunUdpListenerToMasterSlaveGroupHandler implements ExclusiveRequirementHandler {

    private final AliyunUdpListenerHandler listenerHandler;

    private final AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler;

    public AliyunUdpListenerToMasterSlaveGroupHandler(AliyunUdpListenerHandler listenerHandler,
                                                      AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler) {
        this.listenerHandler = listenerHandler;
        this.masterSlaveGroupHandler = masterSlaveGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_UDP_LISTENER_TO_MASTER_SLAVE_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "监听器与主备服务器组";
    }

    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return masterSlaveGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "UDP监听器";
    }

    @Override
    public String getRequirementName() {
        return "主备服务器组";
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
                () -> new StratoException("Listener not found when connecting to master-slave group.")
        );

        AliyunClbMasterSlaveGroup masterSlaveGroup = masterSlaveGroupHandler.describeMasterSlaveGroup(account, group.getExternalId()).orElseThrow(
                () -> new StratoException("Master-Slave group not found when getting connected by listener.")
        );

        var request = new SetLoadBalancerUDPListenerAttributeRequest();
        request.setLoadBalancerId(aliyunListener.listenerId().loadBalancerId());
        request.setListenerPort(Integer.valueOf(aliyunListener.listenerId().port()));

        request.setMasterSlaveServerGroup("on");
        request.setMasterSlaveServerGroupId(masterSlaveGroup.id().masterSlaveGroupId());

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setUdpListenerAttributes(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource listener = relationship.getSource();
        Resource group = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(group.getAccountId());

        Optional<AliyunListener> aliyunListener = listenerHandler.describeListener(account, listener.getExternalId());

        if(aliyunListener.isEmpty())
            return;

        Optional<AliyunClbMasterSlaveGroup> masterSlaveGroup
                = masterSlaveGroupHandler.describeMasterSlaveGroup(account, group.getExternalId());

        if(masterSlaveGroup.isEmpty())
            return;

        var request = new SetLoadBalancerUDPListenerAttributeRequest();
        request.setLoadBalancerId(aliyunListener.get().listenerId().loadBalancerId());
        request.setListenerPort(Integer.valueOf(aliyunListener.get().listenerId().port()));

        request.setMasterSlaveServerGroup("off");

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setUdpListenerAttributes(request);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunListener> aliyunListener = listenerHandler.describeListener(account, source.externalId());

        if(aliyunListener.isEmpty())
            return List.of();

        String groupId = aliyunListener.get().detail().getUDPListenerConfig().getMasterSlaveServerGroupId();

        if(Utils.isBlank(groupId))
            return List.of();

        AliyunClbMasterSlaveGroupId masterSlaveGroupId = new AliyunClbMasterSlaveGroupId(
                aliyunListener.get().listenerId().loadBalancerId(),
                groupId
        );

        Optional<ExternalResource> masterSlaveGroup
                = masterSlaveGroupHandler.describeExternalResource(account, masterSlaveGroupId.toString());

        return masterSlaveGroup.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
