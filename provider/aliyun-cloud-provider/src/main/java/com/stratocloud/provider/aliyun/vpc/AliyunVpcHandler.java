package com.stratocloud.provider.aliyun.vpc;

import com.aliyun.vpc20160428.models.DescribeVpcsRequest;
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
public class AliyunVpcHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunVpcHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_VPC";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云私有网络";
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

    public Optional<AliyunVpc> describeVpc(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeVpc(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunVpc vpc) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                vpc.getVpcId(),
                vpc.detail().getVpcName(),
                convertStatus(vpc.detail().getStatus())
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
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        return client.vpc().describeVpcs(request).stream().map(vpc -> toExternalResource(account, vpc)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunVpc> vpc = describeVpc(account, externalResource.externalId());

        if(vpc.isEmpty())
            return List.of();

        var tags = vpc.get().detail().getTags();
        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunVpc vpc = describeVpc(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Vpc not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, vpc));

        RuntimeProperty cidrProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock",
                "CIDR(IPv4)",
                vpc.detail().getCidrBlock(),
                vpc.detail().getCidrBlock()
        );
        RuntimeProperty ipv6CidrProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock",
                "CIDR(IPv6)",
                vpc.detail().getIpv6CidrBlock(),
                vpc.detail().getIpv6CidrBlock()
        );

        resource.addOrUpdateRuntimeProperty(cidrProperty);
        resource.addOrUpdateRuntimeProperty(ipv6CidrProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
