package com.stratocloud.provider.aliyun.eip;

import com.aliyun.vpc20160428.models.DescribeEipAddressesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEipHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;


    public AliyunEipHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_EIP";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云弹性IP";
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

    public Optional<AliyunEip> describeEip(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeEip(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunEip eip) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                eip.detail().getAllocationId(),
                eip.detail().getName(),
                convertStatus(eip.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String addressStatus) {
        return switch (addressStatus){
            case "Associating" -> ResourceState.ATTACHING;
            case "InUse" -> ResourceState.IN_USE;
            case "Unassociating" -> ResourceState.DETACHING;
            case "Available" -> ResourceState.IDLE;
            case "Releasing" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeEipAddressesRequest request = new DescribeEipAddressesRequest();
        return client.vpc().describeEips(request).stream().map(eip -> toExternalResource(account, eip)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunEip> eip = describeEip(account, externalResource.externalId());

        if(eip.isEmpty())
            return List.of();

        var tags = eip.get().detail().getTags();

        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunEip eip = describeEip(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EIP not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, eip));


        RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                "publicIp", "公网IP", eip.detail().getIpAddress(), eip.detail().getIpAddress()
        );

        String bandwidth = String.valueOf(eip.detail().getBandwidth());
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
