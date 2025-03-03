package com.stratocloud.provider.huawei.servers.actions;

import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiServerDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerDestroyHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ServerDetail> server = serverHandler.describeServer(account, resource.getExternalId());

        if(server.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).ecs().deleteServer(server.get().getId());
    }
}
