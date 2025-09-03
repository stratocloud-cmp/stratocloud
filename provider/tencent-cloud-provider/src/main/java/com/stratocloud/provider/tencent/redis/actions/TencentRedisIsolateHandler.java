package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.RedisUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TencentRedisIsolateHandler implements ResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisIsolateHandler(TencentRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.ISOLATE;
    }

    @Override
    public String getTaskName() {
        return "隔离Redis实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet().stream().filter(
                s -> s != ResourceState.SHUTDOWN
        ).collect(Collectors.toSet());
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STOPPING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        String taskIdOrDealId = provider.buildClient(account).isolateRedisInstance(resource.getExternalId());
        TaskContext.setExternalTaskId(taskIdOrDealId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceSet> instanceSet = client.describeRedisInstance(resource.getExternalId());
        if(instanceSet.isEmpty())
            return ResourceActionResult.finished();

        if(RedisUtil.isPrepaid(instanceSet.get()))
            return RedisUtil.checkDealResult(resource);
        else
            return RedisUtil.checkTaskResult(resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
