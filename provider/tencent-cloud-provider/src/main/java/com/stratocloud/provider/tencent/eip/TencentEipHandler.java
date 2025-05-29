package com.stratocloud.provider.tencent.eip;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import com.tencentcloudapi.vpc.v20170312.models.DescribeAddressesRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentEipHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;


    public TencentEipHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_EIP";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云弹性IP";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.ELASTIC_IP;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeEip(account, externalId).map(
                eip -> toExternalResource(account, eip)
        );
    }

    public Optional<Address> describeEip(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeEip(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Address eip) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                eip.getAddressId(),
                eip.getAddressName(),
                convertStatus(eip.getAddressStatus())
        );
    }

    private ResourceState convertStatus(String addressStatus) {
        return switch (addressStatus){
            case "CREATING" -> ResourceState.BUILDING;
            case "BINDING" -> ResourceState.ATTACHING;
            case "BIND", "BIND_ENI" -> ResourceState.IN_USE;
            case "UNBINDING" -> ResourceState.DETACHING;
            case "UNBIND" -> ResourceState.IDLE;
            case "OFFLINING" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeAddressesRequest request = new DescribeAddressesRequest();
        return client.describeEips(request).stream().map(eip -> toExternalResource(account, eip)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Address> eip = describeEip(account, externalResource.externalId());

        if(eip.isEmpty())
            return List.of();

        if(eip.get().getTagSet() == null)
            return List.of();

        return Arrays.stream(eip.get().getTagSet()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Address eip = describeEip(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EIP not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, eip));


        RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                "publicIp", "公网IP", eip.getAddressIp(), eip.getAddressIp()
        );

        String bandwidth = String.valueOf(eip.getBandwidth());
        RuntimeProperty bandwidthProperty = RuntimeProperty.ofDisplayInList(
                "bandwidth", "带宽(Mbps)", bandwidth, bandwidth
        );

        resource.addOrUpdateRuntimeProperty(publicIpProperty);
        resource.addOrUpdateRuntimeProperty(bandwidthProperty);

        resource.updateUsageByType(UsageTypes.ELASTIC_IP, BigDecimal.ONE);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.ELASTIC_IP);
    }
}
