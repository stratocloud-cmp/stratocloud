package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class AliyunListenerStartHandler implements ResourceActionHandler {

    protected final AliyunListenerHandler listenerHandler;

    protected AliyunListenerStartHandler(AliyunListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.START;
    }

    @Override
    public String getTaskName() {
        return "启动监听器";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.STOPPED);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();

        AliyunListener listener = listenerHandler.describeListener(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );

        provider.buildClient(account).clb().startListener(listener.listenerId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> listener = listenerHandler.describeExternalResource(
                account, resource.getExternalId()
        );

        if(listener.isEmpty())
            return ResourceActionResult.failed("Listener not found after start.");

        if(listener.get().state() == ResourceState.STARTING)
            return ResourceActionResult.inProgress();

        if(listener.get().state() == ResourceState.STOPPED)
            return ResourceActionResult.failed("Failed to start listener.");

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
