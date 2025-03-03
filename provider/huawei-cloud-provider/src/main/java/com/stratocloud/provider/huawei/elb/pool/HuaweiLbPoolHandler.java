package com.stratocloud.provider.huawei.elb.pool;

import com.huaweicloud.sdk.elb.v3.model.ListPoolsRequest;
import com.huaweicloud.sdk.elb.v3.model.Pool;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLbStatusTreeHelper;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiLbPoolHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiLbPoolHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_LB_POOL";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云后端服务器组";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_BACKEND_GROUP;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<Pool> poolV2 = describeLbPool(account, externalId);
        return poolV2.map(p -> toExternalResource(account, p));
    }

    public Optional<Pool> describeLbPool(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describeLbPool(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Pool pool) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                pool.getId(),
                pool.getName(),
                HuaweiLbStatusTreeHelper.getPoolState(provider, account.getId(), pool)
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<Pool> pools = provider.buildClient(account).elb().describeLbPools(
                new ListPoolsRequest()
        );
        return pools.stream().map(
                p -> toExternalResource(account, p)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Pool pool = describeLbPool(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB pool not found.")
        );
        resource.updateByExternal(toExternalResource(account, pool));

        try {
            HuaweiLbStatusTreeHelper.synchronizePoolStatusTree(resource);
        }catch (Exception e){
            log.warn("Failed to synchronize LB pool status: {}.", e.toString());
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
