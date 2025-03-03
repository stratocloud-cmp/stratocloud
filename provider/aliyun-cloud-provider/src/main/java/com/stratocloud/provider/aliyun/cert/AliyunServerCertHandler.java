package com.stratocloud.provider.aliyun.cert;

import com.aliyun.slb20140515.models.DescribeServerCertificatesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunServerCertHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    protected AliyunServerCertHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_CLOUD_SERVER_CERT";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云服务器SSL证书";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SERVER_CERT;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<AliyunServerCert> certificates = describeCert(account, externalId);

        return certificates.map(cert -> toExternalResource(account, cert));
    }

    private Optional<AliyunServerCert> describeCert(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClient client = provider.buildClient(account);
        return client.clb().describeCert(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunServerCert cert) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                cert.detail().getServerCertificateId(),
                cert.detail().getServerCertificateName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeServerCertificatesRequest request = new DescribeServerCertificatesRequest();
        return client.clb().describeCerts(request).stream().map(
                cert -> toExternalResource(account, cert)
        ).toList();
    }


    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource cert = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Cert not found: " + resource.getName())
        );

        resource.updateByExternal(cert);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
