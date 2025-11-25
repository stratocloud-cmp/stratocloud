package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.ChangeInstanceStatusBody;
import com.huaweicloud.sdk.dcs.v2.model.RestartOrFlushInstancesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiRedisRestartHandler implements ResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiRedisRestartHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESTART;
    }

    @Override
    public String getTaskName() {
        return "重启Redis实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.RESTARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RestartInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RestartInput input = JSON.convert(parameters, RestartInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        RestartOrFlushInstancesRequest request = new RestartOrFlushInstancesRequest();

        ChangeInstanceStatusBody body = new ChangeInstanceStatusBody();
        body.setInstances(List.of(resource.getExternalId()));
        body.setAction(input.isForced() ? "restart" : "soft_restart");
        request.setBody(body);

        provider.buildClient(account).dcs().restartOrFlush(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = dcsHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isPresent() && redis.get().state() == ResourceState.RESTARTING)
            return ResourceActionResult.inProgress();


        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Data
    public static class RestartInput implements ResourceActionInput {
        @BooleanField(label = "强制重启")
        private boolean forced;
    }
}
