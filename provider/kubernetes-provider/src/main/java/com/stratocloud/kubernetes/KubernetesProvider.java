package com.stratocloud.kubernetes;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubernetesAccountProperties;
import com.stratocloud.kubernetes.common.KubernetesClient;
import com.stratocloud.kubernetes.common.KubernetesClientImpl;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KubernetesProvider extends AbstractProvider {

    public KubernetesProvider(ExternalAccountRepository accountRepository) {
        super(accountRepository);
    }

    @Override
    public String getId() {
        return "KUBERNETES_PROVIDER";
    }

    @Override
    public String getName() {
        return "Kubernetes";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return KubernetesAccountProperties.class;
    }

    public KubernetesClient buildClient(ExternalAccount account){
        var properties = JSON.convert(account.getProperties(), KubernetesAccountProperties.class);
        return new KubernetesClientImpl(properties.getKubeConfigYaml());
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {
        buildClient(externalAccount).testConnection();
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {

    }
}
