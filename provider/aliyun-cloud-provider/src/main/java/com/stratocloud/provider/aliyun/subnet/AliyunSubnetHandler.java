package com.stratocloud.provider.aliyun.subnet;

import com.aliyun.vpc20160428.models.DescribeVSwitchesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSubnetHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;


    public AliyunSubnetHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_SUBNET";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云子网";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SUBNET;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean isSharedRequirementTarget() {
        return false;
    }

    @Override
    public boolean canAttachIpPool() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSubnet(account, externalId).map(
                subnet -> toExternalResource(account, subnet)
        );
    }

    public Optional<AliyunSubnet> describeSubnet(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).vpc().describeSubnet(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunSubnet subnet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                subnet.detail().getVSwitchId(),
                subnet.detail().getVSwitchName(),
                convertStatus(subnet.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "Pending" -> ResourceState.BUILDING;
            case "Available" -> ResourceState.AVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeVSwitchesRequest request = new DescribeVSwitchesRequest();
        return client.vpc().describeSubnets(request).stream().map(
                subnet -> toExternalResource(account, subnet)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunSubnet> subnet = describeSubnet(account, externalResource.externalId());

        if(subnet.isEmpty())
            return List.of();

        var tags = subnet.get().detail().getTags();
        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunSubnet subnet = describeSubnet(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Subnet not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, subnet));

        RuntimeProperty cidrProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock",
                "CIDR(IPv4)",
                subnet.detail().getCidrBlock(),
                subnet.detail().getCidrBlock()
        );

        RuntimeProperty ipv6CidrProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock",
                "CIDR(IPv6)",
                subnet.detail().getIpv6CidrBlock(),
                subnet.detail().getIpv6CidrBlock()
        );

        resource.addOrUpdateRuntimeProperty(cidrProperty);
        resource.addOrUpdateRuntimeProperty(ipv6CidrProperty);

        Long availableIpAddressCount = subnet.detail().getAvailableIpAddressCount();

        if(availableIpAddressCount != null){
            RuntimeProperty availableIpCountProperty = RuntimeProperty.ofDisplayInList(
                    "availableIpCount",
                    "可用IP情况",
                    availableIpAddressCount.toString(),
                    availableIpAddressCount.toString()
            );

            resource.addOrUpdateRuntimeProperty(availableIpCountProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
