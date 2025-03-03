package com.stratocloud.provider.noncloud;

import com.stratocloud.provider.RuntimePropertiesUtil;
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
public class NonCloudMachineBuildHandler implements BuildResourceActionHandler {

    private final NonCloudMachineHandler machineHandler;

    public NonCloudMachineBuildHandler(NonCloudMachineHandler machineHandler) {
        this.machineHandler = machineHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return machineHandler;
    }

    @Override
    public String getTaskName() {
        return "纳管非云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return NonCloudMachineBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        NonCloudMachineBuildInput input = JSON.convert(parameters, NonCloudMachineBuildInput.class);

        RuntimePropertiesUtil.setManagementIp(resource, input.getIp());
        RuntimePropertiesUtil.setManagementPort(resource, input.getPort());
        RuntimePropertiesUtil.setManagementUser(resource, input.getUsername());
        RuntimePropertiesUtil.setManagementPassword(resource, input.getPassword());
        RuntimePropertiesUtil.setManagementPublicKey(resource, input.getPublicKey());
        RuntimePropertiesUtil.setManagementPrivateKey(resource, input.getPrivateKey());
        RuntimePropertiesUtil.setManagementPassphrase(resource, input.getPassphrase());

        resource.setExternalId(input.getIp());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
