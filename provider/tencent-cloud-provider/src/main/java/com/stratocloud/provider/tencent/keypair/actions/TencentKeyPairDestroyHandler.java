package com.stratocloud.provider.tencent.keypair.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.keypair.TencentKeyPairHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentKeyPairDestroyHandler implements DestroyResourceActionHandler {

    private final TencentKeyPairHandler keyPairHandler;


    public TencentKeyPairDestroyHandler(TencentKeyPairHandler keyPairHandler) {
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
        if(Utils.isBlank(resource.getExternalId()))
            return;

        TencentCloudProvider provider = (TencentCloudProvider) keyPairHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        provider.buildClient(account).deleteKeyPair(resource.getExternalId());
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        List<Resource> instances = resource.getRequirementTargets(ResourceCategories.COMPUTE_INSTANCE).stream().filter(
                r -> ResourceState.getAliveStateSet().contains(r.getState())
        ).toList();

        if(Utils.isNotEmpty(instances))
            throw new StratoException("Detach id pair from all instances first.");
    }
}
