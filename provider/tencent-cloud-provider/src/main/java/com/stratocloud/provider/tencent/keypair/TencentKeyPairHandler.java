package com.stratocloud.provider.tencent.keypair;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.DescribeKeyPairsRequest;
import com.tencentcloudapi.cvm.v20170312.models.KeyPair;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentKeyPairHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentKeyPairHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_KEYPAIR";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云密钥对";
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

    public Optional<KeyPair> describeKeyPair(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeKeyPair(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, KeyPair keyPair) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                keyPair.getKeyId(),
                keyPair.getKeyName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
        return client.describeKeyPairs(request).stream().map(keyPair -> toExternalResource(account, keyPair)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<KeyPair> keyPair = describeKeyPair(account, externalResource.externalId());

        if(keyPair.isEmpty())
            return List.of();

        if(keyPair.get().getTags() == null)
            return List.of();

        return Arrays.stream(keyPair.get().getTags()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        KeyPair keyPair = describeKeyPair(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Key pair not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, keyPair));

        RuntimePropertiesUtil.setManagementPublicKey(resource, keyPair.getPublicKey());
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
