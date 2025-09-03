package com.stratocloud.provider.tencent.redis;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class RedisUtil {
    public static boolean isPrepaid(InstanceSet instanceSet) {
        return Objects.equals(instanceSet.getBillingMode(), 1L);
    }

    public static ResourceActionResult checkDealResult(Resource resource) {
        Optional<String> dealId = TaskContext.getExternalTaskId();
        if(dealId.isEmpty())
            return ResourceActionResult.finished();

        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        var redisDeal = client.describeRedisDeal(dealId.get());

        if(redisDeal.isEmpty() || redisDeal.get().getStatus() == null)
            return ResourceActionResult.finished();

        return switch (redisDeal.get().getStatus().intValue()) {
            case 5 -> ResourceActionResult.failed(redisDeal.get().getDescription());
            case 3 -> ResourceActionResult.inProgress();
            default -> ResourceActionResult.finished();
        };
    }

    public static ResourceActionResult checkTaskResult(Resource resource) {
        Optional<String> taskId = TaskContext.getExternalTaskId();
        if(taskId.isEmpty())
            return ResourceActionResult.finished();

        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        long redisTaskId;

        try {
            redisTaskId = Long.parseLong(taskId.get());
        }catch (Exception e){
            log.warn("Unexpected redis task id: {}", taskId.get());
            return ResourceActionResult.finished();
        }

        var redisTask = client.describeRedisTask(redisTaskId);

        if(redisTask.isEmpty() || redisTask.get().getStatus() == null)
            return ResourceActionResult.finished();

        return switch (redisTask.get().getStatus()) {
            case "failed", "error" -> ResourceActionResult.failed(redisTask.get().getTaskMessage());
            case "preparing", "running" -> ResourceActionResult.inProgress();
            default -> ResourceActionResult.finished();
        };
    }
}
