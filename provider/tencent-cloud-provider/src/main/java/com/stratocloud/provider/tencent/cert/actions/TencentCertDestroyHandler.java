package com.stratocloud.provider.tencent.cert.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cert.TencentServerCertHandler;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentCertDestroyHandler implements DestroyResourceActionHandler {

    private final TencentServerCertHandler certHandler;

    public TencentCertDestroyHandler(TencentServerCertHandler certHandler) {
        this.certHandler = certHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return certHandler;
    }

    @Override
    public String getTaskName() {
        return "删除SSL证书";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> cert = certHandler.describeExternalResource(account, resource.getExternalId());

        if(cert.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) certHandler.getProvider();
        provider.buildClient(account).deleteCertificate(cert.get().externalId());
    }
}
