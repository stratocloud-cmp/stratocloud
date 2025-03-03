package com.stratocloud.provider.noncloud;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.repository.ExternalAccountRepository;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NonCloudProvider extends AbstractProvider {

    public NonCloudProvider(ExternalAccountRepository accountRepository) {
        super(accountRepository);
    }

    @Override
    public String getId() {
        return "NON_CLOUD_PROVIDER";
    }

    @Override
    public String getName() {
        return "非云资源池";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return NonCloudAccountProperties.class;
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {

    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {

    }

    @Data
    public static class NonCloudAccountProperties implements ExternalAccountProperties {

    }
}
