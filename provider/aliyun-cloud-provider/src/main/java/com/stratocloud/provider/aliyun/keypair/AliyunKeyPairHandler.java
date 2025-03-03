package com.stratocloud.provider.aliyun.keypair;

import com.aliyun.ecs20140526.models.DescribeKeyPairsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
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
public class AliyunKeyPairHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunKeyPairHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_KEYPAIR";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云密钥对";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.KEY_PAIR;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeKeyPair(account, externalId).map(
                keyPair -> toExternalResource(account, keyPair)
        );
    }

    public Optional<AliyunKeyPair> describeKeyPair(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeKeyPair(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunKeyPair keyPair) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                keyPair.detail().getKeyPairName(),
                keyPair.detail().getKeyPairName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
        return client.ecs().describeKeyPairs(request).stream().map(
                keyPair -> toExternalResource(account, keyPair)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunKeyPair> keyPair = describeKeyPair(account, externalResource.externalId());

        if(keyPair.isEmpty())
            return List.of();

        var tags = keyPair.get().detail().getTags();

        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(
                        new TagEntry(tag.getTagKey(), tag.getTagKey()), tag.getTagValue(), tag.getTagValue(),
                        0
                )
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunKeyPair keyPair = describeKeyPair(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Key pair not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, keyPair));

        RuntimePropertiesUtil.setManagementPublicKey(resource, keyPair.detail().getPublicKey());
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
