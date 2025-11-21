package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiDcsDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiDcsDestroyHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁Redis实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceListInfo> instance = dcsHandler.describeInstance(account, resource.getExternalId());
        if(instance.isEmpty())
            return;

        provider.buildClient(account).dcs().deleteInstance(instance.get().getInstanceId());
    }
}
