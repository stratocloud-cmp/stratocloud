package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.job.TaskContext;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.redis.RedisUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceState;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentRedisDestroyHandler implements DestroyResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisDestroyHandler(TencentRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁Redis实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceSet> instanceSet = redisHandler.describeRedis(account, resource.getExternalId());

        if(instanceSet.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        String taskId = provider.buildClient(account).cleanUpRedisInstance(instanceSet.get().getInstanceId());
        TaskContext.setExternalTaskId(taskId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = RedisUtil.checkTaskResult(resource);

        if(result.taskState() == TaskState.FINISHED)
            return DestroyResourceActionHandler.super.checkActionResult(resource, parameters);
        else
            return result;
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = redisHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isPresent() && redis.get().state() != ResourceState.SHUTDOWN)
            throw new BadCommandException("请先隔离Redis实例");
    }
}
