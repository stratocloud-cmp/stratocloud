package com.stratocloud.provider.tencent.lb.listener.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.cert.TencentServerCertHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL7ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListener;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.CertificateOutput;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentL7ListenerToCertHandler implements ExclusiveRequirementHandler {

    private final TencentL7ListenerHandler listenerHandler;

    private final TencentServerCertHandler certHandler;

    public TencentL7ListenerToCertHandler(TencentL7ListenerHandler listenerHandler,
                                          TencentServerCertHandler certHandler) {
        this.listenerHandler = listenerHandler;
        this.certHandler = certHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_L7_LISTENER_TO_CERT_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "七层监听器与证书";
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
        return "七层监听器";
    }

    @Override
    public String getRequirementName() {
        return "SSL证书";
    }

    @Override
    public String getConnectActionName() {
        return "绑定证书";
    }

    @Override
    public String getDisconnectActionName() {
        return "解绑证书";
    }

    @Override
    public void connect(Relationship relationship) {

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
        Optional<TencentListener> listener = listenerHandler.describeListener(account, source.externalId());

        if(listener.isEmpty())
            return List.of();

        CertificateOutput certificate = listener.get().listener().getCertificate();

        if(certificate == null || Utils.isBlank(certificate.getCertId()))
            return List.of();

        Optional<ExternalResource> cert = certHandler.describeExternalResource(account, certificate.getCertId());

        return cert.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
