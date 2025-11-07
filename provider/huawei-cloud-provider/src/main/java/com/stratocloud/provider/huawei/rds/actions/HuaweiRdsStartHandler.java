package com.stratocloud.provider.huawei.rds.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.huawei.rds.HuaweiRdsUtil;
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
public class HuaweiRdsStartHandler implements ResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsStartHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.START;
    }

    @Override
    public String getTaskName() {
        return "启动RDS实例";
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
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String jobId = provider.buildClient(account).rds().startInstance(resource.getExternalId());
        TaskContext.setExternalTaskId(jobId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return HuaweiRdsUtil.checkActionResult(resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
