package com.stratocloud.provider.tencent.cert.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cert.TencentServerCertHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.ssl.v20191205.models.ApplyCertificateRequest;
import com.tencentcloudapi.ssl.v20191205.models.CreateCertificateRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TencentCertBuildHandler implements BuildResourceActionHandler {

    private final TencentServerCertHandler certHandler;

    public TencentCertBuildHandler(TencentServerCertHandler certHandler) {
        this.certHandler = certHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return certHandler;
    }

    @Override
    public String getTaskName() {
        return "创建SSL证书";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentCertBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCertBuildInput input = JSON.convert(parameters, TencentCertBuildInput.class);

        if(input.getUseFreeCert())
            applyFreeCert(resource, input);
         else
            createCert(resource, input);
    }

    private void createCert(Resource resource, TencentCertBuildInput input) {
        CreateCertificateRequest request = new CreateCertificateRequest();

        request.setProductId(input.getProductId());
        request.setDomainNum(input.getDomainNum());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) certHandler.getProvider();
        String certId = provider.buildClient(account).createCertificate(request);
        resource.setExternalId(certId);
    }

    private void applyFreeCert(Resource resource, TencentCertBuildInput input) {
        ApplyCertificateRequest request = new ApplyCertificateRequest();


        request.setAlias(resource.getName());

        request.setDvAuthMethod(input.getDvAuthMethod());
        request.setDomainName(input.getDomainName());
        request.setPackageType(input.getPackageType());

        if(Utils.isNotBlank(input.getContactEmail()))
            request.setContactEmail(input.getContactEmail());

        if(Utils.isNotBlank(input.getContactPhone()))
            request.setContactPhone(input.getContactPhone());

        if(Utils.isNotBlank(input.getCsrEncryptAlgo())){
            request.setCsrEncryptAlgo(input.getCsrEncryptAlgo());

            if(Objects.equals("RSA", input.getCsrEncryptAlgo()))
                request.setCsrKeyParameter("2048");
            else if(Objects.equals("ECC", input.getCsrEncryptAlgo()))
                request.setCsrKeyParameter("prime256v1");

            request.setCsrKeyPassword(input.getCsrKeyPassword());
        }

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) certHandler.getProvider();
        String certId = provider.buildClient(account).applyFreeCertificate(request);
        resource.setExternalId(certId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
