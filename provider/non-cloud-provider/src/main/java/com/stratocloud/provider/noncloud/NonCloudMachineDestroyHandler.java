package com.stratocloud.provider.noncloud;

import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NonCloudMachineDestroyHandler implements DestroyResourceActionHandler {

    private final NonCloudMachineHandler machineHandler;

    public NonCloudMachineDestroyHandler(NonCloudMachineHandler machineHandler) {
        this.machineHandler = machineHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return machineHandler;
    }

    @Override
    public String getTaskName() {
        return "解除纳管非云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        resource.onDestroyed();
        return ResourceActionResult.finished();
    }
}
