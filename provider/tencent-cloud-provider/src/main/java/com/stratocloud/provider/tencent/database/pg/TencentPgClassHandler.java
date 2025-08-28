package com.stratocloud.provider.tencent.database.pg;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.util.PgInstanceClass;
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentPgClassHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentPgClassHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_PG_CLASS";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云PostgreSQL规格";
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
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<PgInstanceClass> instanceClass = describePgClass(account, externalId);

        return instanceClass.map(c -> toExternalResource(account, c));
    }

    public Optional<PgInstanceClass> describePgClass(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describePgClass(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, PgInstanceClass instanceClass) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instanceClass.detail().getSpecCode(),
                PgUtil.getInstanceClassName(instanceClass),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describePgClasses().stream().map(
                c -> toExternalResource(account, c)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        PgInstanceClass instanceClass = describePgClass(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("PG instance class not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, instanceClass));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
