package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.aliyun.slb20140515.models.SetLoadBalancerStatusRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunIntranetClbStartHandler implements ResourceActionHandler {

    private final AliyunIntranetClbHandler clbHandler;

    public AliyunIntranetClbStartHandler(AliyunIntranetClbHandler clbHandler) {
        this.clbHandler = clbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return clbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.START;
    }

    @Override
    public String getTaskName() {
        return "启动CLB";
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
        AliyunCloudProvider provider = (AliyunCloudProvider) clbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        SetLoadBalancerStatusRequest request = new SetLoadBalancerStatusRequest();
        request.setLoadBalancerId(resource.getExternalId());
        request.setLoadBalancerStatus("active");

        provider.buildClient(account).clb().setLoadBalancerStatus(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
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
