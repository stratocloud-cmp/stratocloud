package com.stratocloud.provider.huawei.keypair.read;

import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.huawei.keypair.HuaweiKeyPairHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class HuaweiKeyPairReadPrivateKeyHandler implements ResourceReadActionHandler {

    private final HuaweiKeyPairHandler keyPairHandler;

    public HuaweiKeyPairReadPrivateKeyHandler(HuaweiKeyPairHandler keyPairHandler) {
        this.keyPairHandler = keyPairHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return keyPairHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_PRIVATE_KEY;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.values());
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource){
        Optional<String> privateKey = RuntimePropertiesUtil.getManagementPrivateKey(resource);

        return privateKey.map(
                s -> List.of(
                        new ResourceReadActionResult("私钥", s, false)
                )
        ).orElseGet(List::of);
    }
}
