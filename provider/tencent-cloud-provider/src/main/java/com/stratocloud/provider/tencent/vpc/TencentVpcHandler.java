package com.stratocloud.provider.tencent.vpc;

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
import com.tencentcloudapi.vpc.v20170312.models.DescribeVpcsRequest;
import com.tencentcloudapi.vpc.v20170312.models.Vpc;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentVpcHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentVpcHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_VPC";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云私有网络";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.VPC;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean isSharedRequirementTarget() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeVpc(account, externalId).map(
                vpc -> toExternalResource(account, vpc)
        );
    }

    public Optional<Vpc> describeVpc(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeVpc(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Vpc vpc) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                vpc.getVpcId(),
                vpc.getVpcName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        return client.describeVpcs(request).stream().map(vpc -> toExternalResource(account, vpc)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Vpc> vpc = describeVpc(account, externalResource.externalId());

        if(vpc.isEmpty())
            return List.of();

        if(vpc.get().getTagSet() == null)
            return List.of();

        return Arrays.stream(vpc.get().getTagSet()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Vpc vpc = describeVpc(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Vpc not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, vpc));

        RuntimeProperty cidrProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock", "CIDR(IPv4)", vpc.getCidrBlock(), vpc.getCidrBlock()
        );
        RuntimeProperty ipv6CidrProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock", "CIDR(IPv6)", vpc.getIpv6CidrBlock(), vpc.getIpv6CidrBlock()
        );

        resource.addOrUpdateRuntimeProperty(cidrProperty);
        resource.addOrUpdateRuntimeProperty(ipv6CidrProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
