package com.stratocloud.provider.tencent.instance.read;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.resource.*;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentInstanceReadConsoleUrlHandler implements ResourceReadActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public static final String PREFIX = "https://img.qcloud.com/qcloud/app/active_vnc/index.html?InstanceVncUrl=";

    public static final String ORCA_TERM_URL_FORMAT = "https://orcaterm.cloud.tencent.com/terminal?type=cvm&instanceId=%s&region=%s&from=cvm_console_login_btn";

    public TencentInstanceReadConsoleUrlHandler(TencentInstanceHandler instanceHandler) {
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
        Optional<Instance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return List.of();

        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);
        String consoleUrl = client.describeInstanceConsoleUrl(instance.get().getInstanceId());

        return List.of(
                new ResourceReadActionResult(
                        "VNC终端", PREFIX+consoleUrl, true
                ),
                new ResourceReadActionResult(
                        "OrcaTerm终端",
                        ORCA_TERM_URL_FORMAT.formatted(
                                instance.get().getInstanceId(),
                                client.getRegion()
                        ),
                        true
                )
        );
    }
}
