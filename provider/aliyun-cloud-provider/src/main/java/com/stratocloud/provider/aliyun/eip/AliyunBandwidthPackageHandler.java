package com.stratocloud.provider.aliyun.eip;

import com.aliyun.vpc20160428.models.DescribeCommonBandwidthPackagesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunBandwidthPackageHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;


    public AliyunBandwidthPackageHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_BANDWIDTH_PACKAGE";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云带宽包";
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

    public Optional<AliyunBandwidthPackage> describePackage(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeBandwidthPackage(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunBandwidthPackage bandwidthPackage) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                bandwidthPackage.detail().getBandwidthPackageId(),
                bandwidthPackage.detail().getName(),
                convertStatus(bandwidthPackage.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "Available" -> ResourceState.AVAILABLE;
            case "Modifying" -> ResourceState.CONFIGURING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeCommonBandwidthPackagesRequest request = new DescribeCommonBandwidthPackagesRequest();
        return client.vpc().describeBandwidthPackages(request).stream().map(
                bandwidthPackage -> toExternalResource(account, bandwidthPackage)
        ).toList();
    }


    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunBandwidthPackage bandwidthPackage = describePackage(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("BandwidthPackage not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, bandwidthPackage));


        String bandwidth = String.valueOf(bandwidthPackage.detail().getBandwidth());
        RuntimeProperty bandwidthProperty = RuntimeProperty.ofDisplayInList(
                "bandwidth", "带宽包限速大小(Mbps)", bandwidth, bandwidth
        );
        resource.addOrUpdateRuntimeProperty(bandwidthProperty);

        String expiredTime = bandwidthPackage.detail().getExpiredTime();
        if(Utils.isNotBlank(expiredTime)){
            RuntimeProperty expiredTimeProperty = RuntimeProperty.ofDisplayInList(
                    "expiredTime", "到期时间", expiredTime, expiredTime
            );
            resource.addOrUpdateRuntimeProperty(expiredTimeProperty);
        }


        var chargeType = AliyunBwpChargeType.fromChargeType(bandwidthPackage.detail().getInternetChargeType());

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
