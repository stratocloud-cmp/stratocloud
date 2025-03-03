package com.stratocloud.provider.huawei.eip.actions;

import com.huaweicloud.sdk.eip.v2.model.PublicipShowResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.eip.HuaweiEipHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiEipDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiEipHandler eipHandler;

    public HuaweiEipDestroyHandler(HuaweiEipHandler eipHandler) {
        this.eipHandler = eipHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return eipHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁弹性IP";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<PublicipShowResp> eip = eipHandler.describeEip(account, resource.getExternalId());

        if(eip.isEmpty())
            return;

        provider.buildClient(account).eip().deleteEip(eip.get().getId());
    }
}
