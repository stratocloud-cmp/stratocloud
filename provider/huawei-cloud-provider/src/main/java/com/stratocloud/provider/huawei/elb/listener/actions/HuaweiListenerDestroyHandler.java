package com.stratocloud.provider.huawei.elb.listener.actions;

import com.huaweicloud.sdk.elb.v3.model.Listener;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.listener.HuaweiListenerHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiListenerDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiListenerHandler listenerHandler;

    public HuaweiListenerDestroyHandler(HuaweiListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<Listener> listener = listenerHandler.describeListener(account, resource.getExternalId());

        if(listener.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) listenerHandler.getProvider();

        provider.buildClient(account).elb().deleteListener(listener.get().getId());
    }
}
