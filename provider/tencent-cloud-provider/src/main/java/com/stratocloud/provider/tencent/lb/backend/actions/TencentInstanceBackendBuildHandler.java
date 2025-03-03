package com.stratocloud.provider.tencent.lb.backend.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendHandler;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendId;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentInstanceBackendToL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentInstanceBackendToL7RuleHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.clb.v20180317.models.RegisterTargetsRequest;
import com.tencentcloudapi.clb.v20180317.models.Target;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentInstanceBackendBuildHandler implements BuildResourceActionHandler {


    private final TencentInstanceBackendHandler backendHandler;

    public TencentInstanceBackendBuildHandler(TencentInstanceBackendHandler backendHandler) {
        this.backendHandler = backendHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return backendHandler;
    }

    @Override
    public String getTaskName() {
        return "添加后端服务";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentInstanceBackendBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource instance = resource.getEssentialTarget(ResourceCategories.COMPUTE_INSTANCE).orElseThrow(
                () -> new StratoException("Compute instance not found when creating backend.")
        );

        Optional<Resource> listener = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_LISTENER);

        Optional<Resource> rule = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_RULE);

        TencentInstanceBackendBuildInput input = JSON.convert(parameters, TencentInstanceBackendBuildInput.class);

        if(listener.isEmpty() && rule.isPresent()){
            registerToRule(resource, instance, rule.get(), input);
        }else if(listener.isPresent() && rule.isEmpty()){
            registerToListener(resource, instance, listener.get(), input);
        }else if(listener.isPresent()){
            throw new StratoException("后端服务不能同时绑定监听器与转发规则");
        }else {
            throw new StratoException("后端服务必须指定监听器或转发规则");
        }
    }

    private void registerToListener(Resource backend,
                                    Resource instance,
                                    Resource listener,
                                    TencentInstanceBackendBuildInput input) {
        TencentListenerId listenerId = TencentListenerId.fromString(listener.getExternalId());

        RegisterTargetsRequest request = new RegisterTargetsRequest();

        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerId(listenerId.listenerId());


        Target target = new Target();
        target.setPort(input.getPort());
        target.setInstanceId(instance.getExternalId());
        target.setWeight(input.getWeight());

        request.setTargets(new Target[]{target});

        ExternalAccount account = getAccountRepository().findExternalAccount(backend.getAccountId());

        TencentCloudProvider provider = (TencentCloudProvider) backendHandler.getProvider();
        provider.buildClient(account).registerTarget(request);

        TencentInstanceBackendId backendId = new TencentInstanceBackendId(
                listenerId.lbId(), listenerId.listenerId(), instance.getExternalId()
        );

        backend.setExternalId(backendId.toString());
    }

    private void registerToRule(Resource backend,
                                Resource instance,
                                Resource rule,
                                TencentInstanceBackendBuildInput input) {
        TencentL7RuleId ruleId = TencentL7RuleId.fromString(rule.getExternalId());

        RegisterTargetsRequest request = new RegisterTargetsRequest();

        request.setLoadBalancerId(ruleId.lbId());
        request.setListenerId(ruleId.listenerId());
        request.setLocationId(ruleId.locationId());


        Target target = new Target();
        target.setPort(input.getPort());
        target.setInstanceId(instance.getExternalId());
        target.setWeight(input.getWeight());

        request.setTargets(new Target[]{target});

        ExternalAccount account = getAccountRepository().findExternalAccount(backend.getAccountId());

        TencentCloudProvider provider = (TencentCloudProvider) backendHandler.getProvider();
        provider.buildClient(account).registerTarget(request);

        TencentInstanceBackendId backendId = new TencentInstanceBackendId(
                ruleId.lbId(), ruleId.listenerId(), instance.getExternalId()
        );

        backend.setExternalId(backendId.toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        Optional<Resource> listener = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER);

        Optional<Resource> rule = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_RULE);

        if(listener.isPresent() && rule.isPresent()){
            throw new StratoException("后端服务不能同时绑定监听器与转发规则");
        }else if(listener.isEmpty() && rule.isEmpty()) {
            throw new StratoException("后端服务必须指定监听器或转发规则");
        }
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentInstanceBackendToL4ListenerHandler.TYPE_ID,
                TencentInstanceBackendToL7RuleHandler.TYPE_ID
        );
    }
}
