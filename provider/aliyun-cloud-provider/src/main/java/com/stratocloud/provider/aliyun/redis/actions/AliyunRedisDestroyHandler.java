package com.stratocloud.provider.aliyun.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunRedisDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisDestroyHandler(AliyunRedisHandler redisHandler) {
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
        Optional<RedisInstance> redis = redisHandler.describeRedis(account, resource.getExternalId());

        if(redis.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();

        provider.buildClient(account).tair().deleteInstance(resource.getExternalId());
    }
}
