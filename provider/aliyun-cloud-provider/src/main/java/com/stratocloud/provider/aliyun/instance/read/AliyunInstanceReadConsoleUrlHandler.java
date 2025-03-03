package com.stratocloud.provider.aliyun.instance.read;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunInstanceReadConsoleUrlHandler implements ResourceReadActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public static final String VNC_URL_FORMAT = "https://g.alicdn.com/aliyun/ecs-console-vnc2/0.0.8/index.html?instanceId=%s&vncUrl=%s&isWindows=%s";

    public static final String WORKBENCH_URL_FORMAT = "https://ecs-workbench.aliyun.com/?from=ecs&instanceType=ecs&regionId=%s&instanceId=%s&resourceGroupId=&language=zh-CN";

    public AliyunInstanceReadConsoleUrlHandler(AliyunInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_CONSOLE_URL;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.STARTED);
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return List.of();

        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        var instanceDetail = instance.get().detail();
        String instanceId = instanceDetail.getInstanceId();

        String consoleUrl = provider.buildClient(account).ecs().describeInstanceConsoleUrl(
                instanceId
        );

        return List.of(
                new ResourceReadActionResult(
                        "VNC终端",
                        VNC_URL_FORMAT.formatted(
                                instanceId,
                                consoleUrl,
                                instance.get().isWindows()
                        ),
                        true
                ),
                new ResourceReadActionResult(
                        "Workbench终端",
                        WORKBENCH_URL_FORMAT.formatted(
                                instanceDetail.getRegionId(),
                                instanceId
                        ),
                        true
                )
        );
    }
}
