package com.stratocloud.provider.huawei;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.provider.huawei.common.HuaweiCloudAccountProperties;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.HuaweiCloudClientImpl;
import com.stratocloud.provider.huawei.metrics.HuaweiMetricsProvider;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiCloudProvider extends AbstractProvider {

    private final CacheService cacheService;

    private final HuaweiMetricsProvider metricsProvider;

    public HuaweiCloudProvider(ExternalAccountRepository accountRepository,
                               CacheService cacheService,
                               HuaweiMetricsProvider metricsProvider) {
        super(accountRepository);
        this.cacheService = cacheService;
        this.metricsProvider = metricsProvider;
    }

    @Override
    public String getId() {
        return "HUAWEI_CLOUD_PROVIDER";
    }

    @Override
    public String getName() {
        return "华为云";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return HuaweiCloudAccountProperties.class;
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {
        buildClient(externalAccount).validateConnection();
    }

    public HuaweiCloudClient buildClient(ExternalAccount externalAccount){
        var properties = JSON.convert(externalAccount.getProperties(), HuaweiCloudAccountProperties.class);
        return new HuaweiCloudClientImpl(cacheService, properties);
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {
        properties.remove("secretAccessKey");
    }

    @Override
    public Float getBalance(ExternalAccount account) {
        return buildClient(account).describeBalance();
    }


    @Override
    public Optional<MetricsProvider> getMetricsProvider() {
        return Optional.of(metricsProvider);
    }
}
