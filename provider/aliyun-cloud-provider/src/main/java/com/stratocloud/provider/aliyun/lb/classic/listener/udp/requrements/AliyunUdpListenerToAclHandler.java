package com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements;

import com.aliyun.slb20140515.models.SetLoadBalancerUDPListenerAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.acl.AliyunClbAclHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToAclHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToAclInput;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AliyunUdpListenerToAclHandler extends AliyunListenerToAclHandler {

    public AliyunUdpListenerToAclHandler(AliyunUdpListenerHandler listenerHandler,
                                         AliyunClbAclHandler aclHandler) {
        super(listenerHandler, aclHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_CLB_UDP_LISTENER_TO_ACL_RELATIONSHIP";
    }

    @Override
    public String getCapabilityName() {
        return "UDP监听器";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource acl = relationship.getTarget();
        Resource listener = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(listener.getAccountId());

        AliyunListener aliyunListener = listenerHandler.describeListener(account, listener.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );

        String aclId = aliyunListener.detail().getAclId();
        AliyunListenerId listenerId = aliyunListener.listenerId();

        if(Objects.equals(aclId, acl.getExternalId())){
            log.info("Acl {} is already used by {}, skipping...", aclId, listenerId);
            return;
        }

        if(Utils.isNotBlank(aclId) && !Objects.equals(aclId, acl.getExternalId()))
            throw new StratoException("Listener %s is using another acl.".formatted(listenerId));

        var input = JSON.convert(relationship.getProperties(), AliyunListenerToAclInput.class);

        var request = new SetLoadBalancerUDPListenerAttributeRequest();

        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerPort(Integer.valueOf(listenerId.port()));
        request.setAclId(aclId);
        request.setAclType(input.getAclType());
        request.setAclStatus("on");

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setUdpListenerAttributes(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource acl = relationship.getTarget();
        Resource listener = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(listener.getAccountId());

        Optional<AliyunListener> aliyunListener = listenerHandler.describeListener(account, listener.getExternalId());

        if(aliyunListener.isEmpty())
            return;

        String aclId = aliyunListener.get().detail().getAclId();

        AliyunListenerId listenerId = aliyunListener.get().listenerId();

        if(Utils.isBlank(aclId)){
            log.info("Acl {} is not used by {}, skipping...", aclId, listenerId);
            return;
        }

        if(Utils.isNotBlank(aclId) && !Objects.equals(aclId, acl.getExternalId()))
            throw new StratoException("Listener %s is using another acl.".formatted(listenerId));

        var request = new SetLoadBalancerUDPListenerAttributeRequest();

        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerPort(Integer.valueOf(listenerId.port()));
        request.setAclStatus("off");

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setUdpListenerAttributes(request);
    }
}
