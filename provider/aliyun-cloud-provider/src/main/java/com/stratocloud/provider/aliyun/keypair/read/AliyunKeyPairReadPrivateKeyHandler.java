package com.stratocloud.provider.aliyun.keypair.read;

import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.aliyun.keypair.AliyunKeyPairHandler;
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
public class AliyunKeyPairReadPrivateKeyHandler implements ResourceReadActionHandler {

    private final AliyunKeyPairHandler keyPairHandler;

    public AliyunKeyPairReadPrivateKeyHandler(AliyunKeyPairHandler keyPairHandler) {
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

        if(privateKey.isEmpty())
            return List.of();

        try {
            return List.of(new ResourceReadActionResult("私钥", privateKey.get(), false));
        }catch (Exception e){
            log.warn("Failed to retrieve private key of {}.", resource.getName());
            return List.of();
        }

    }
}
