package com.stratocloud.provider.huawei.keypair.actions;

import com.huaweicloud.sdk.kps.v3.model.Keypairs;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.keypair.HuaweiKeyPairHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiKeyPairDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiKeyPairHandler keyPairHandler;

    public HuaweiKeyPairDestroyHandler(HuaweiKeyPairHandler keyPairHandler) {
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return keyPairHandler;
    }

    @Override
    public String getTaskName() {
        return "删除密钥对";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<Keypairs> keypair = keyPairHandler.describeKeyPair(account, resource.getExternalId());

        if(keypair.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) keyPairHandler.getProvider();
        provider.buildClient(account).kps().deleteKeyPair(keypair.get().getKeypair().getName());
    }
}
