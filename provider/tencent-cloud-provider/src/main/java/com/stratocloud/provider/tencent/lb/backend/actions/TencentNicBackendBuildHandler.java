package com.stratocloud.provider.tencent.lb.backend.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendId;
import com.stratocloud.provider.tencent.lb.backend.TencentNicBackendHandler;
import com.stratocloud.provider.tencent.lb.backend.TencentNicBackendId;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentNicBackendToL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.backend.requirements.TencentNicBackendToL7RuleHandler;
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
public class TencentNicBackendBuildHandler implements BuildResourceActionHandler {

    private final TencentNicBackendHandler backendHandler;

    public TencentNicBackendBuildHandler(TencentNicBackendHandler backendHandler) {
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
        return TencentNicBackendBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Optional<Resource> listener = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_LISTENER);

        Optional<Resource> rule = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER_RULE);

        TencentNicBackendBuildInput input = JSON.convert(parameters, TencentNicBackendBuildInput.class);

        if(listener.isEmpty() && rule.isPresent()){
            registerToRule(resource, rule.get(), input);
        }else if(listener.isPresent() && rule.isEmpty()){
            registerToListener(resource, listener.get(), input);
        }else if(listener.isPresent()){
            throw new StratoException("后端服务不能同时绑定监听器与转发规则");
        }else {
            throw new StratoException("后端服务必须指定监听器或转发规则");
        }
    }

    private void registerToListener(Resource backend,
                                    Resource listener,
                                    TencentNicBackendBuildInput input) {


        TencentListenerId listenerId = TencentListenerId.fromString(listener.getExternalId());

        RegisterTargetsRequest request = new RegisterTargetsRequest();

        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerId(listenerId.listenerId());


        Target target = new Target();
        target.setPort(input.getPort());
        target.setWeight(input.getWeight());
        target.setEniIp(input.getIp());

        request.setTargets(new Target[]{target});


        ExternalAccount account = getAccountRepository().findExternalAccount(backend.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) backendHandler.getProvider();

        provider.buildClient(account).registerTarget(request);

        TencentNicBackendId backendId = new TencentNicBackendId(
                listenerId.lbId(), listenerId.listenerId(), input.getIp()
        );

        backend.setExternalId(backendId.toString());
    }

    private void registerToRule(Resource backend,
                                Resource rule,
                                TencentNicBackendBuildInput input) {
        TencentL7RuleId ruleId = TencentL7RuleId.fromString(rule.getExternalId());

        RegisterTargetsRequest request = new RegisterTargetsRequest();

        request.setLoadBalancerId(ruleId.lbId());
        request.setListenerId(ruleId.listenerId());
        request.setLocationId(ruleId.locationId());


        Target target = new Target();
        target.setPort(input.getPort());
        target.setEniIp(input.getIp());
        target.setWeight(input.getWeight());

        request.setTargets(new Target[]{target});

        ExternalAccount account = getAccountRepository().findExternalAccount(backend.getAccountId());

        TencentCloudProvider provider = (TencentCloudProvider) backendHandler.getProvider();
        provider.buildClient(account).registerTarget(request);

        TencentInstanceBackendId backendId = new TencentInstanceBackendId(
                ruleId.lbId(), ruleId.listenerId(), input.getIp()
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
                TencentNicBackendToL4ListenerHandler.TYPE_ID,
                TencentNicBackendToL7RuleHandler.TYPE_ID
        );
    }
}
