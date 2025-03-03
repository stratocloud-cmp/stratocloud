package com.stratocloud.provider.tencent.lb.backend.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.backend.TencentBackend;
import com.stratocloud.provider.tencent.lb.backend.TencentNicBackendHandler;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentNicBackendToL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentNicBackendToL7RuleHandler;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.clb.v20180317.models.DeregisterTargetsRequest;
import com.tencentcloudapi.clb.v20180317.models.Target;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicBackendDestroyHandler implements DestroyResourceActionHandler {

    private final TencentNicBackendHandler backendHandler;

    public TencentNicBackendDestroyHandler(TencentNicBackendHandler backendHandler) {
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<TencentBackend> backend = backendHandler.describeBackend(account, resource.getExternalId());

        if(backend.isEmpty())
            return;

        Optional<Resource> listener = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_LISTENER);

        Optional<Resource> rule = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_RULE);

        if(listener.isEmpty() && rule.isEmpty())
            return;

        if(listener.isPresent() && rule.isPresent())
            throw new StratoException("Invalid backend.");

        DeregisterTargetsRequest request = new DeregisterTargetsRequest();

        request.setLoadBalancerId(backend.get().lbId());
        request.setListenerId(backend.get().listenerId());

        if(rule.isPresent()){
            TencentL7RuleId ruleId = TencentL7RuleId.fromString(rule.get().getExternalId());
            request.setLocationId(ruleId.locationId());
        }

        Target target = new Target();
        target.setEniIp(backend.get().backend().getPrivateIpAddresses()[0]);

        request.setTargets(new Target[]{target});

        TencentCloudProvider provider = (TencentCloudProvider) backendHandler.getProvider();
        provider.buildClient(account).deregisterTarget(request);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentNicBackendToL4ListenerHandler.TYPE_ID,
                TencentNicBackendToL7RuleHandler.TYPE_ID
        );
    }
}
