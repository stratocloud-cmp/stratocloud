package com.stratocloud.provider.huawei.keypair;

import com.huaweicloud.sdk.kps.v3.model.Keypairs;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiKeyPairHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiKeyPairHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_KEY_PAIR";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云密钥对";
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
                keypair -> toExternalResource(account, keypair)
        );
    }

    public Optional<Keypairs> describeKeyPair(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).kps().describeKeyPair(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Keypairs keypair) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                keypair.getKeypair().getName(),
                keypair.getKeypair().getName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        var client = provider.buildClient(account);
        return client.kps().describeKeyPairs().stream().map(
                keypair -> toExternalResource(account, keypair)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Keypairs keypair = describeKeyPair(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("KeyPair not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, keypair));

        RuntimeProperty publicKeyProperty = RuntimeProperty.of(
                "publicKey",
                "公钥",
                keypair.getKeypair().getPublicKey(),
                keypair.getKeypair().getPublicKey(),
                true, false, false
        );
        resource.addOrUpdateRuntimeProperty(publicKeyProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
