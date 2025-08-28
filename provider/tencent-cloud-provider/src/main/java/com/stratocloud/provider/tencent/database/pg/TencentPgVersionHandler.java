package com.stratocloud.provider.tencent.database.pg;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.Version;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentPgVersionHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentPgVersionHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_PG_VERSION";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云PostgreSQL版本";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.DB_INSTANCE_FLAVOR;
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
        Optional<Version> version = describePgVersion(account, externalId);

        return version.map(v -> toExternalResource(account, v));
    }

    public Optional<Version> describePgVersion(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describePgVersion(externalId);
    }

    public ExternalResource toExternalResource(ExternalAccount account, Version version) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                version.getDBKernelVersion(),
                "%s %s".formatted(version.getDBEngine(), version.getDBKernelVersion()),
                convertState(version.getStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status) {
            case "AVAILABLE" -> ResourceState.AVAILABLE;
            case "UPGRADE_ONLY", "DEPRECATED" -> ResourceState.UNAVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describePgVersions().stream().map(
                v -> toExternalResource(account, v)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Version version = describePgVersion(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("PG version not found: " + resource.getExternalId())
        );

        ExternalResource versionResource = toExternalResource(account, version);
        resource.updateByExternal(versionResource);

        if(versionResource.state() == ResourceState.UNAVAILABLE)
            resource.markRecycled(false);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
