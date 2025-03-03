package com.stratocloud.provider.tencent.securitygroup;

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
import com.tencentcloudapi.vpc.v20170312.models.DescribeSecurityGroupsRequest;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentSecurityGroupHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentSecurityGroupHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_SECURITY_GROUP";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云安全组";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SECURITY_GROUP;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSecurityGroup(account, externalId).map(
                securityGroup -> toExternalResource(account, securityGroup)
        );
    }

    public Optional<SecurityGroup> describeSecurityGroup(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeSecurityGroup(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, SecurityGroup securityGroup) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                securityGroup.getSecurityGroupId(),
                securityGroup.getSecurityGroupName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        return client.describeSecurityGroups(request).stream().map(
                securityGroup -> toExternalResource(account, securityGroup)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<SecurityGroup> securityGroup = describeSecurityGroup(account, externalResource.externalId());

        if(securityGroup.isEmpty())
            return List.of();

        if(securityGroup.get().getTagSet() == null)
            return List.of();

        return Arrays.stream(securityGroup.get().getTagSet()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource securityGroup = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("SecurityGroup not found: " + resource.getName())
        );

        resource.updateByExternal(securityGroup);

        Set<String> policyCategories = Set.of(
                ResourceCategories.SECURITY_GROUP_EGRESS_POLICY.id(),
                ResourceCategories.SECURITY_GROUP_INGRESS_POLICY.id()
        );

        List<Relationship> policyRelationships = resource.getCapabilities().stream().filter(
                rel -> policyCategories.contains(rel.getSource().getCategory())
        ).toList();

        for (Relationship relationship : policyRelationships) {
            Resource policy = relationship.getSource();

            policy.synchronize();

            if(policy.getState() != ResourceState.DESTROYED) {
                relationship.onConnected();
                policy.restore();
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }
}
