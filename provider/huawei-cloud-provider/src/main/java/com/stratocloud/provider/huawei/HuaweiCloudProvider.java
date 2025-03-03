package com.stratocloud.provider.huawei;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.provider.huawei.common.HuaweiCloudAccountProperties;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.HuaweiCloudClientImpl;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HuaweiCloudProvider extends AbstractProvider {

    private final CacheService cacheService;

    public HuaweiCloudProvider(ExternalAccountRepository accountRepository,
                               CacheService cacheService) {
        super(accountRepository);
        this.cacheService = cacheService;
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
}
