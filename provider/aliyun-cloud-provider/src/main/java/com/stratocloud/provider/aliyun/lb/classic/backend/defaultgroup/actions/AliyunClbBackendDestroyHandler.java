package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.actions;

import com.aliyun.slb20140515.models.RemoveBackendServersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackend;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackendHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackendId;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunClbBackendDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunClbBackendHandler backendHandler;

    public AliyunClbBackendDestroyHandler(AliyunClbBackendHandler backendHandler) {
        this.backendHandler = backendHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return backendHandler;
    }

    @Override
    public String getTaskName() {
        return "移除后端服务";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) backendHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunClbBackend> backend = backendHandler.describeBackend(account, resource.getExternalId());

        if(backend.isEmpty())
            return;

        RemoveBackendServersRequest request = new RemoveBackendServersRequest();

        Map<String, Object> backendServer = new HashMap<>();

        AliyunClbBackendId backendId = backend.get().id();

        backendServer.put("ServerId", backendId.resourceId());
        backendServer.put("Weight", backendId.weight().toString());
        backendServer.put("Type", backendId.resourceType());

        request.setBackendServers(JSON.toJsonString(List.of(backendServer)));
        request.setLoadBalancerId(backendId.loadBalancerId());

        provider.buildClient(account).clb().removeBackendServers(request);
    }
}
