package com.stratocloud.kubernetes.volume.actions;

import com.stratocloud.kubernetes.volume.KubernetesPodVolumeHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KubernetesPodVolumeDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesPodVolumeHandler volumeHandler;

    public KubernetesPodVolumeDestroyHandler(KubernetesPodVolumeHandler volumeHandler) {
        this.volumeHandler = volumeHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return volumeHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Volume";
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
