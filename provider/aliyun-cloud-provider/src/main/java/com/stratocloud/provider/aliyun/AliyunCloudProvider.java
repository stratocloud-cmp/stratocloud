package com.stratocloud.provider.aliyun;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.provider.aliyun.common.AliyunAccountProperties;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunClientImpl;
import com.stratocloud.provider.aliyun.metric.AliyunMetricsProvider;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunCloudProvider extends AbstractProvider {

    private final CacheService cacheService;

    private final AliyunMetricsProvider metricsProvider;


    public AliyunCloudProvider(ExternalAccountRepository accountRepository,
                               CacheService cacheService,
                               AliyunMetricsProvider metricsProvider) {
        super(accountRepository);
        this.cacheService = cacheService;
        this.metricsProvider = metricsProvider;
    }

    @Override
    public String getId() {
        return "ALIYUN_PROVIDER";
    }

    @Override
    public String getName() {
        return "阿里云";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return AliyunAccountProperties.class;
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {
        buildClient(externalAccount).validateConnection();
    }


    public AliyunClient buildClient(ExternalAccount externalAccount){
        var properties = JSON.convert(externalAccount.getProperties(), AliyunAccountProperties.class);
        return new AliyunClientImpl(properties, cacheService);
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {
        properties.remove("accessKeySecret");
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
