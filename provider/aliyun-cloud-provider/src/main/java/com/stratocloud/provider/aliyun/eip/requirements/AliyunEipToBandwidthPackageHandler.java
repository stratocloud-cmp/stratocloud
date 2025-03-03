package com.stratocloud.provider.aliyun.eip.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackageHandler;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.aliyun.eip.AliyunEipHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunEipToBandwidthPackageHandler implements ExclusiveRequirementHandler {

    private final AliyunEipHandler eipHandler;

    private final AliyunBandwidthPackageHandler packageHandler;

    public AliyunEipToBandwidthPackageHandler(AliyunEipHandler eipHandler,
                                              AliyunBandwidthPackageHandler packageHandler) {
        this.eipHandler = eipHandler;
        this.packageHandler = packageHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_EIP_TO_BANDWIDTH_PACKAGE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "弹性IP与带宽包";
    }

    @Override
    public ResourceHandler getSource() {
        return eipHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return packageHandler;
    }

    @Override
    public String getCapabilityName() {
        return "弹性IP";
    }

    @Override
    public String getRequirementName() {
        return "带宽包";
    }

    @Override
    public String getConnectActionName() {
        return "绑定带宽包";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource bandwidthPackage = relationship.getTarget();
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

        AliyunEip aliyunEip = eipHandler.describeEip(account, eip.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EIP not found: " + eip.getExternalId())
        );

        if(Objects.equals(
                aliyunEip.detail().getBandwidthPackageId(),
                bandwidthPackage.getExternalId()
        ))
            return;

        provider.buildClient(account).vpc().addBandwidthPackageIp(
                bandwidthPackage.getExternalId(),
                eip.getExternalId()
        );
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource bandwidthPackage = relationship.getTarget();
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).vpc().removeBandwidthPackageIp(
                bandwidthPackage.getExternalId(),
                eip.getExternalId()
        );
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunEip> eip = eipHandler.describeEip(account, source.externalId());

        if(eip.isEmpty())
            return List.of();

        String packageId = eip.get().detail().getBandwidthPackageId();

        Optional<ExternalResource> bandwidthPackage = packageHandler.describeExternalResource(account, packageId);

        if(bandwidthPackage.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                bandwidthPackage.get(),
                Map.of()
        ));
    }
}
