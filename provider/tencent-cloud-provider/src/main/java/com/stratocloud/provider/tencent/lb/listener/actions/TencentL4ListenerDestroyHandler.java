package com.stratocloud.provider.tencent.lb.listener.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.listener.TencentL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListener;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL4ListenerToInternalLbHandler;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL4ListenerToOpenLbHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentL4ListenerDestroyHandler implements DestroyResourceActionHandler {

    private final TencentL4ListenerHandler listenerHandler;

    public TencentL4ListenerDestroyHandler(TencentL4ListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "删除四层监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<TencentListener> listener = listenerHandler.describeListener(account, resource.getExternalId());

        if(listener.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) listenerHandler.getProvider();
        provider.buildClient(account).deleteListener(TencentListenerId.fromString(resource.getExternalId()));
    }


    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentL4ListenerToInternalLbHandler.TYPE_ID,
                TencentL4ListenerToOpenLbHandler.TYPE_ID
        );
    }
}
