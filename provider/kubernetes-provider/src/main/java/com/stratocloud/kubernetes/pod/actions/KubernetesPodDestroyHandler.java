package com.stratocloud.kubernetes.pod.actions;

import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KubernetesPodDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesPodHandler podHandler;

    public KubernetesPodDestroyHandler(KubernetesPodHandler podHandler) {
        this.podHandler = podHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return podHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Pod";
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
