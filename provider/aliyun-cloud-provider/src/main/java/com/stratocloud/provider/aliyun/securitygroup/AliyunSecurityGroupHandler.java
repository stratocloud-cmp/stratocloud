package com.stratocloud.provider.aliyun.securitygroup;

import com.aliyun.ecs20140526.models.DescribeSecurityGroupsRequest;
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
import java.util.Set;

@Component
public class AliyunSecurityGroupHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunSecurityGroupHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_SECURITY_GROUP";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云安全组";
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
    public boolean isSharedRequirementTarget() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSecurityGroup(account, externalId).map(
                securityGroup -> toExternalResource(account, securityGroup)
        );
    }

    public Optional<AliyunSecurityGroup> describeSecurityGroup(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeSecurityGroup(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunSecurityGroup securityGroup) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                securityGroup.detail().getSecurityGroupId(),
                securityGroup.detail().getSecurityGroupName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        return client.ecs().describeSecurityGroups(request).stream().map(
                securityGroup -> toExternalResource(account, securityGroup)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunSecurityGroup> securityGroup = describeSecurityGroup(account, externalResource.externalId());

        if(securityGroup.isEmpty())
            return List.of();

        var tags = securityGroup.get().detail().getTags();

        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getTagKey(), tag.getTagKey()), tag.getTagValue(), tag.getTagValue(), 0)
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
