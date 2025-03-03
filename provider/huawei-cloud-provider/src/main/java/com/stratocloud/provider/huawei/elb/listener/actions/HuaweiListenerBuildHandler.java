package com.stratocloud.provider.huawei.elb.listener.actions;

import com.huaweicloud.sdk.elb.v3.model.CreateListenerOption;
import com.huaweicloud.sdk.elb.v3.model.CreateListenerRequest;
import com.huaweicloud.sdk.elb.v3.model.CreateListenerRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.listener.HuaweiListenerHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class HuaweiListenerBuildHandler implements BuildResourceActionHandler {

    private final HuaweiListenerHandler listenerHandler;

    public HuaweiListenerBuildHandler(HuaweiListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiListenerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiListenerBuildInput input = JSON.convert(parameters, HuaweiListenerBuildInput.class);
        Resource lb = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating listener.")
        );

        CreateListenerOption option = new CreateListenerOption();
        option.withName(resource.getName()).withDescription(resource.getDescription())
                .withProtocol(input.getProtocol()).withProtocolPort(input.getPort())
                .withLoadbalancerId(lb.getExternalId());

        Set<String> securedProtocols = Set.of(
                "TERMINATED_HTTPS", "HTTPS", "TLS", "QUIC"
        );

        if(securedProtocols.contains(input.getProtocol()))
            option.withDefaultTlsContainerRef(input.getDefaultTlsContainerRef());

        option.withEnhanceL7policyEnable(input.getEnhancePolicyEnabled());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) listenerHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String listenerId = provider.buildClient(account).elb().createListener(
                new CreateListenerRequest().withBody(
                        new CreateListenerRequestBody().withListener(option)
                )
        );
        resource.setExternalId(listenerId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
