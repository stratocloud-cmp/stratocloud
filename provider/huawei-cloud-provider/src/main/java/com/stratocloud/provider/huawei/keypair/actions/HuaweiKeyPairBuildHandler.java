package com.stratocloud.provider.huawei.keypair.actions;

import com.huaweicloud.sdk.kps.v3.model.CreateKeypairAction;
import com.huaweicloud.sdk.kps.v3.model.CreateKeypairRequest;
import com.huaweicloud.sdk.kps.v3.model.CreateKeypairRequestBody;
import com.huaweicloud.sdk.kps.v3.model.CreateKeypairResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.keypair.HuaweiKeyPairHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiKeyPairBuildHandler implements BuildResourceActionHandler {

    private final HuaweiKeyPairHandler keyPairHandler;

    public HuaweiKeyPairBuildHandler(HuaweiKeyPairHandler keyPairHandler) {
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
        return HuaweiKeyPairBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, HuaweiKeyPairBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) keyPairHandler.getProvider();

        CreateKeypairRequest request = new CreateKeypairRequest().withBody(
                new CreateKeypairRequestBody().withKeypair(
                        new CreateKeypairAction()
                                .withName(resource.getName())
                                .withPublicKey(input.getPublicKey())
                                .withType(CreateKeypairAction.TypeEnum.fromValue(input.getType()))
                )
        );

        CreateKeypairResp resp = provider.buildClient(account).kps().createKeyPair(request);
        resource.setExternalId(resource.getName());

        RuntimePropertiesUtil.setManagementPrivateKey(resource, resp.getPrivateKey());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
