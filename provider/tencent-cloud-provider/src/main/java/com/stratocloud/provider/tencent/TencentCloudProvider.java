package com.stratocloud.provider.tencent;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.tencent.common.TencentCloudAccountProperties;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentCloudClientImpl;
import com.stratocloud.provider.tencent.metric.TencentMetricsProvider;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentCloudProvider extends AbstractProvider {

    private final CacheService cacheService;

    private final TencentMetricsProvider metricsProvider;

    public TencentCloudProvider(ExternalAccountRepository accountRepository,
                                CacheService cacheService,
                                TencentMetricsProvider metricsProvider) {
        super(accountRepository);
        this.cacheService = cacheService;
        this.metricsProvider = metricsProvider;
    }

    @Override
    public String getId() {
        return "TENCENT_CLOUD";
    }

    @Override
    public String getName() {
        return "腾讯云";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return TencentCloudAccountProperties.class;
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {
        buildClient(externalAccount).validateConnection();
    }


    public TencentCloudClient buildClient(ExternalAccount externalAccount){
        var properties = JSON.convert(externalAccount.getProperties(), TencentCloudAccountProperties.class);
        return new TencentCloudClientImpl(properties, cacheService);
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {
        properties.remove("secretKey");
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
