package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunClbServerGroupDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunClbServerGroupHandler serverGroupHandler;

    public AliyunClbServerGroupDestroyHandler(AliyunClbServerGroupHandler serverGroupHandler) {
        this.serverGroupHandler = serverGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "删除服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) serverGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunClbServerGroup> serverGroup
                = serverGroupHandler.describeServerGroup(account, resource.getExternalId());

        if(serverGroup.isEmpty())
            return;

        provider.buildClient(account).clb().deleteServerGroup(
                serverGroup.get().id()
        );
    }
}
