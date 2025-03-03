package com.stratocloud.provider.tencent.cert;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.ssl.v20191205.models.Certificates;
import com.tencentcloudapi.ssl.v20191205.models.DescribeCertificatesRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentServerCertHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    protected TencentServerCertHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_SERVER_CERT";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云服务器SSL证书";
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
        Optional<Certificates> certificates = describeCert(account, externalId);

        return certificates.map(cert -> toExternalResource(account, cert));
    }

    private Optional<Certificates> describeCert(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeCert(externalId).map(
                cert -> isServerCert(cert) ? cert : null
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, Certificates cert) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                cert.getCertificateId(),
                cert.getAlias(),
                convertStatus(cert.getStatus())
        );
    }

    private ResourceState convertStatus(long status) {
        if (status == 1L)
            return ResourceState.AVAILABLE;
        return ResourceState.UNAVAILABLE;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeCertificatesRequest request = new DescribeCertificatesRequest();
        return client.describeCerts(request).stream().filter(
                this::isServerCert
        ).map(
                cert -> toExternalResource(account, cert)
        ).toList();
    }


    protected boolean isServerCert(Certificates certificates){
        return Objects.equals("SVR", certificates.getCertificateType());
    }


    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Certificates cert = describeCert(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Cert not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, cert));

        RuntimeProperty statusProperty = RuntimeProperty.ofDisplayInList(
                "status", "证书状态", String.valueOf(cert.getStatus()), cert.getStatusName()
        );
        resource.addOrUpdateRuntimeProperty(statusProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
