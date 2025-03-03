package com.stratocloud.provider.tencent.eip.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentBandwidthPackageHandler;
import com.stratocloud.provider.tencent.eip.TencentEipHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentEipToBandwidthPackageHandler implements ExclusiveRequirementHandler {

    private final TencentEipHandler eipHandler;

    private final TencentBandwidthPackageHandler packageHandler;

    public TencentEipToBandwidthPackageHandler(TencentEipHandler eipHandler,
                                               TencentBandwidthPackageHandler packageHandler) {
        this.eipHandler = eipHandler;
        this.packageHandler = packageHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_EIP_TO_BANDWIDTH_PACKAGE_RELATIONSHIP";
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
        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();

        Address address = eipHandler.describeEip(account, eip.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EIP not found: " + eip.getExternalId())
        );

        if(Objects.equals(address.getBandwidthPackageId(), bandwidthPackage.getExternalId()))
            return;

        provider.buildClient(account).addBandwidthPackageResource(
                bandwidthPackage.getExternalId(),
                eip.getExternalId(),
                "Address"
        );
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource bandwidthPackage = relationship.getTarget();
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).removeBandwidthPackageResource(
                bandwidthPackage.getExternalId(),
                eip.getExternalId(),
                "Address"
        );
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<Address> address = eipHandler.describeEip(account, source.externalId());

        if(address.isEmpty())
            return List.of();

        String packageId = address.get().getBandwidthPackageId();

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
