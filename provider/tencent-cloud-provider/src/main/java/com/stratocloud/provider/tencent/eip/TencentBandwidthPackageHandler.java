package com.stratocloud.provider.tencent.eip;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.eip.actions.TencentBandwidthChargeType;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.BandwidthPackage;
import com.tencentcloudapi.vpc.v20170312.models.DescribeBandwidthPackagesRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentBandwidthPackageHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;


    public TencentBandwidthPackageHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_BANDWIDTH_PACKAGE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云带宽包";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.BANDWIDTH_PACKAGE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describePackage(account, externalId).map(
                bandwidthPackage -> toExternalResource(account, bandwidthPackage)
        );
    }

    public Optional<BandwidthPackage> describePackage(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeBandwidthPackage(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, BandwidthPackage bandwidthPackage) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                bandwidthPackage.getBandwidthPackageId(),
                bandwidthPackage.getBandwidthPackageName(),
                convertStatus(bandwidthPackage.getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "CREATING" -> ResourceState.BUILDING;
            case "CREATED" -> ResourceState.AVAILABLE;
            case "DELETING" -> ResourceState.DESTROYING;
            case "DELETED" -> ResourceState.DESTROYED;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeBandwidthPackagesRequest request = new DescribeBandwidthPackagesRequest();
        return client.describeBandwidthPackages(request).stream().map(
                bandwidthPackage -> toExternalResource(account, bandwidthPackage)
        ).toList();
    }


    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        BandwidthPackage bandwidthPackage = describePackage(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("BandwidthPackage not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, bandwidthPackage));


        String bandwidth = String.valueOf(bandwidthPackage.getBandwidth());
        RuntimeProperty bandwidthProperty = RuntimeProperty.ofDisplayInList(
                "bandwidth", "带宽包限速大小(Mbps)", bandwidth, bandwidth
        );
        resource.addOrUpdateRuntimeProperty(bandwidthProperty);

        if(Utils.isNotBlank(bandwidthPackage.getDeadline())){
            RuntimeProperty deadlineProperty = RuntimeProperty.ofDisplayInList(
                    "deadline", "到期时间", bandwidthPackage.getDeadline(), bandwidthPackage.getDeadline()
            );
            resource.addOrUpdateRuntimeProperty(deadlineProperty);
        }


        var chargeType = TencentBandwidthChargeType.fromChargeType(bandwidthPackage.getChargeType());

        if(chargeType.isPresent()){
            RuntimeProperty chargeTypeProperty = RuntimeProperty.ofDisplayInList(
                    "chargeType", "计费方式", chargeType.get().getId(), chargeType.get().getName()
            );
            resource.addOrUpdateRuntimeProperty(chargeTypeProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }


}
