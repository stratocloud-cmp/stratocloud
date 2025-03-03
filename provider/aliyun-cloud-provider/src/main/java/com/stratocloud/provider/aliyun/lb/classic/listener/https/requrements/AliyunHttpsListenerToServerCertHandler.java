package com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements;

import com.aliyun.slb20140515.models.SetLoadBalancerHTTPSListenerAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.cert.AliyunServerCertHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerId;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AliyunHttpsListenerToServerCertHandler implements ExclusiveRequirementHandler {

    private final AliyunHttpsListenerHandler listenerHandler;

    private final AliyunServerCertHandler certHandler;

    public AliyunHttpsListenerToServerCertHandler(AliyunHttpsListenerHandler listenerHandler,
                                                  AliyunServerCertHandler certHandler) {
        this.listenerHandler = listenerHandler;
        this.certHandler = certHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_CLB_HTTPS_LISTENER_TO_SERVER_CERT_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云HTTPS监听器与服务器证书";
    }

    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return certHandler;
    }

    @Override
    public String getCapabilityName() {
        return "HTTPS监听器";
    }

    @Override
    public String getRequirementName() {
        return "服务器证书";
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
        Resource cert = relationship.getTarget();
        Resource listener = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(listener.getAccountId());

        AliyunListener aliyunListener = listenerHandler.describeListener(account, listener.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );

        String certId = aliyunListener.detail().getHTTPSListenerConfig().getServerCertificateId();
        AliyunListenerId listenerId = aliyunListener.listenerId();

        if(Objects.equals(certId, cert.getExternalId())){
            log.info("Cert {} is already used by {}, skipping...", certId, listenerId);
            return;
        }

        var request = new SetLoadBalancerHTTPSListenerAttributeRequest();

        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerPort(Integer.valueOf(listenerId.port()));
        request.setServerCertificateId(certId);

        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).clb().setHttpsListenerAttributes(request);
    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunListener> listener = listenerHandler.describeListener(account, source.externalId());
        if(listener.isEmpty())
            return List.of();

        String certId = listener.get().detail().getHTTPSListenerConfig().getServerCertificateId();

        if(Utils.isBlank(certId))
            return List.of();

        Optional<ExternalResource> cert = certHandler.describeExternalResource(account, certId);

        if(cert.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        cert.get(),
                        Map.of()
                )
        );
    }
}
