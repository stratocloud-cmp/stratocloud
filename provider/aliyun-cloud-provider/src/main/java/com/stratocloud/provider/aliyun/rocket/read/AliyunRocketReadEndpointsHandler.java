package com.stratocloud.provider.aliyun.rocket.read;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.rocket.AliyunRocketHandler;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunRocketReadEndpointsHandler implements ResourceReadActionHandler {

    private final AliyunRocketHandler rocketHandler;

    public AliyunRocketReadEndpointsHandler(AliyunRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_ENDPOINTS;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<RocketInstance> rocketInstance = rocketHandler.describeInstance(account, resource.getExternalId());

        if(rocketInstance.isEmpty())
            return List.of();

        AliyunCloudProvider provider = (AliyunCloudProvider) rocketHandler.getProvider();

        var detail = provider.buildClient(account).rocket().describeInstanceDetail(
                rocketInstance.get().detail().getInstanceId()
        );

        var networkInfo = detail.getNetworkInfo();

        if(networkInfo == null || Utils.isEmpty(networkInfo.getEndpoints()))
            return List.of();

        return networkInfo.getEndpoints().stream().map(
                e -> new ResourceReadActionResult(
                        Objects.equals(e.getEndpointType(), "TCP_INTERNET") ? "公网地址":"内网地址",
                        e.getEndpointUrl(),
                        false,
                        ResourceReadActionResult.ResultType.PLAIN_TEXT
                )
        ).toList();
    }
}
