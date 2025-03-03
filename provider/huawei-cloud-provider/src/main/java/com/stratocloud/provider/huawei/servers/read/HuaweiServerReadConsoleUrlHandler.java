package com.stratocloud.provider.huawei.servers.read;

import com.huaweicloud.sdk.ecs.v2.model.InterfaceAttachment;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.vpc.v2.model.FixedIp;
import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiServerReadConsoleUrlHandler implements ResourceReadActionHandler {

    public static final String CLOUD_SHELL_FORMAT
            = "https://shell.huaweicloud.com/remote?id=%s&name=%s&is_private=true&private_ip=%s&port_id=%s&vpc_id=%s";
    private final HuaweiServerHandler serverHandler;

    public HuaweiServerReadConsoleUrlHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
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

        Optional<ServerDetail> server = serverHandler.describeServer(account, resource.getExternalId());

        if(server.isEmpty())
            return List.of();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        String consoleUrl = client.ecs().describeServerConsoleUrl(resource.getExternalId());

        ResourceReadActionResult vncResult = new ResourceReadActionResult(
                "VNC终端",
                consoleUrl,
                true
        );

        Optional<ResourceReadActionResult> cloudShellResult = getCloudShellResult(client, resource);

        return cloudShellResult.map(
                shellResult -> List.of(vncResult, shellResult)
        ).orElseGet(() -> List.of(vncResult));
    }

    private Optional<ResourceReadActionResult> getCloudShellResult(HuaweiCloudClient client,
                                                                   Resource server) {
        if(serverHandler.getOsTypeQuietly(server) != OsType.Linux)
            return Optional.empty();

        List<InterfaceAttachment> attachments = client.ecs().listServerInterfaces(server.getExternalId());

        if(Utils.isEmpty(attachments))
            return Optional.empty();

        Optional<Port> port = client.vpc().describePort(attachments.get(0).getPortId());

        if(port.isEmpty())
            return Optional.empty();

        String networkId = port.get().getNetworkId();

        Optional<Subnet> subnet = client.vpc().describeSubnet(networkId);

        if(subnet.isEmpty())
            return Optional.empty();

        List<FixedIp> fixedIps = port.get().getFixedIps();

        if(Utils.isEmpty(fixedIps))
            return Optional.empty();

        String ipAddress = fixedIps.get(0).getIpAddress();

        return Optional.of(
                new ResourceReadActionResult(
                        "CloudShell终端",
                        CLOUD_SHELL_FORMAT.formatted(
                                server.getExternalId(),
                                server.getName(),
                                ipAddress,
                                port.get().getId(),
                                subnet.get().getVpcId()
                        ),
                        true
                )
        );
    }
}
