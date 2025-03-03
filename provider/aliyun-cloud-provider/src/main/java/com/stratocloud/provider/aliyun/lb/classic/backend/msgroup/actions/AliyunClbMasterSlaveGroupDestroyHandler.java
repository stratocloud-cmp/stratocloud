package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunClbMasterSlaveGroupDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler;

    public AliyunClbMasterSlaveGroupDestroyHandler(AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler) {
        this.masterSlaveGroupHandler = masterSlaveGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return masterSlaveGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "删除主备服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) masterSlaveGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunClbMasterSlaveGroup> masterSlaveGroup
                = masterSlaveGroupHandler.describeMasterSlaveGroup(account, resource.getExternalId());

        if(masterSlaveGroup.isEmpty())
            return;

        provider.buildClient(account).clb().deleteMasterSlaveGroup(
                masterSlaveGroup.get().id()
        );
    }
}
