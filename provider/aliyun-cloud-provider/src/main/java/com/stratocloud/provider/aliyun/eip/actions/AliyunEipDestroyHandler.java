package com.stratocloud.provider.aliyun.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.aliyun.eip.AliyunEipHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEipDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunEipHandler eipHandler;

    public AliyunEipDestroyHandler(AliyunEipHandler eipHandler) {
        this.eipHandler = eipHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return eipHandler;
    }

    @Override
    public String getTaskName() {
        return "删除弹性IP";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunEip> eip = eipHandler.describeEip(account, resource.getExternalId());
        if(eip.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).vpc().deleteEip(resource.getExternalId());
    }
}
