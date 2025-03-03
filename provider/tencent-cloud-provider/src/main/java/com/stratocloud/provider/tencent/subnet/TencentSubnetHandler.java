package com.stratocloud.provider.tencent.subnet;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.DescribeSubnetsRequest;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentSubnetHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;


    public TencentSubnetHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_SUBNET";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云子网";
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

    public Optional<Subnet> describeSubnet(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).describeSubnet(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Subnet subnet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                subnet.getSubnetId(),
                subnet.getSubnetName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        return client.describeSubnets(request).stream().map(
                subnet -> toExternalResource(account, subnet)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Subnet> subnet = describeSubnet(account, externalResource.externalId());

        if(subnet.isEmpty())
            return List.of();

        if(subnet.get().getTagSet() == null)
            return List.of();

        return Arrays.stream(subnet.get().getTagSet()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Subnet subnet = describeSubnet(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Subnet not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, subnet));

        RuntimeProperty cidrProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock", "CIDR(IPv4)", subnet.getCidrBlock(), subnet.getCidrBlock()
        );

        RuntimeProperty ipv6CidrProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock", "CIDR(IPv6)", subnet.getIpv6CidrBlock(), subnet.getIpv6CidrBlock()
        );

        String availableIpCount = "%s/%s".formatted(subnet.getAvailableIpAddressCount(), subnet.getTotalIpAddressCount());
        RuntimeProperty availableIpCountProperty = RuntimeProperty.ofDisplayInList(
                "availableIpCount", "可用IP情况", availableIpCount, availableIpCount
        );

        resource.addOrUpdateRuntimeProperty(cidrProperty);
        resource.addOrUpdateRuntimeProperty(ipv6CidrProperty);
        resource.addOrUpdateRuntimeProperty(availableIpCountProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
