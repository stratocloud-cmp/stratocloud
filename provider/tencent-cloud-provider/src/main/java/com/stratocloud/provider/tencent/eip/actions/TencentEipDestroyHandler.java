package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentEipHandler;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentEipDestroyHandler implements DestroyResourceActionHandler {

    private final TencentEipHandler eipHandler;

    public TencentEipDestroyHandler(TencentEipHandler eipHandler) {
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
        Optional<Address> eip = eipHandler.describeEip(account, resource.getExternalId());
        if(eip.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).deleteEip(resource.getExternalId());
    }
}
