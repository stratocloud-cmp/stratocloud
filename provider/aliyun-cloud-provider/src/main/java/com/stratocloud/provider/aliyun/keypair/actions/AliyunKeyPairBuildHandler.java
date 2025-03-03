package com.stratocloud.provider.aliyun.keypair.actions;

import com.aliyun.ecs20140526.models.CreateKeyPairRequest;
import com.aliyun.ecs20140526.models.CreateKeyPairResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.keypair.AliyunKeyPairHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunKeyPairBuildHandler implements BuildResourceActionHandler {

    private final AliyunKeyPairHandler keyPairHandler;


    public AliyunKeyPairBuildHandler(AliyunKeyPairHandler keyPairHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) keyPairHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyPairName(resource.getName());

        CreateKeyPairResponseBody responseBody = provider.buildClient(account).ecs().createKeyPair(request);

        resource.setExternalId(resource.getName());

        RuntimePropertiesUtil.setManagementPrivateKey(resource, responseBody.getPrivateKeyBody());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
