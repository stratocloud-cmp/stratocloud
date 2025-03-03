package com.stratocloud.provider.tencent.keypair.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.keypair.TencentKeyPairHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.tencentcloudapi.cvm.v20170312.models.CreateKeyPairRequest;
import com.tencentcloudapi.cvm.v20170312.models.KeyPair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentKeyPairBuildHandler implements BuildResourceActionHandler {

    private final TencentKeyPairHandler keyPairHandler;


    public TencentKeyPairBuildHandler(TencentKeyPairHandler keyPairHandler) {
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return keyPairHandler;
    }


    @Override
    public String getTaskName() {
        return "创建密钥对";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) keyPairHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyName(resource.getName().replaceAll("-", ""));

        KeyPair keyPair = provider.buildClient(account).createKeyPair(request);

        resource.setExternalId(keyPair.getKeyId());

        RuntimePropertiesUtil.setManagementPublicKey(resource, keyPair.getPublicKey());

        RuntimePropertiesUtil.setManagementPrivateKey(resource, keyPair.getPrivateKey());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
