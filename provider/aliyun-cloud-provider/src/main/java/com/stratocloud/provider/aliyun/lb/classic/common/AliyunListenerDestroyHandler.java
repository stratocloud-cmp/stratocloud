package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;

import java.util.Map;
import java.util.Optional;

public abstract class AliyunListenerDestroyHandler implements DestroyResourceActionHandler {

    protected final AliyunListenerHandler listenerHandler;

    protected AliyunListenerDestroyHandler(AliyunListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "删除监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();

        Optional<AliyunListener> listener = listenerHandler.describeListener(account, resource.getExternalId());

        if(listener.isEmpty())
            return;

        provider.buildClient(account).clb().deleteListener(listener.get().listenerId());
    }


}
