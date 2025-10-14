package com.stratocloud.provider.aliyun.redis.actions;

import com.aliyun.r_kvstore20150101.models.RestartInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
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
public class AliyunRedisRestartHandler implements ResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisRestartHandler(AliyunRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
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

        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        RestartInstanceRequest request = new RestartInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setEffectiveTime(input.getEffectiveTime());
        request.setUpgradeMinorVersion(input.isUpgradeMinorVersion());

        provider.buildClient(account).tair().restartInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = redisHandler.describeExternalResource(account, resource.getExternalId());

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
        @SelectField(
                label = "生效时间",
                options = {
                        "Immediately",
                        "MaintainTime"
                },
                optionNames = {
                        "立即重启",
                        "可运维时间段内重启"
                },
                defaultValues = "Immediately"
        )
        private String effectiveTime;

        @BooleanField(label = "重启时将小版本升级到最新版")
        private boolean upgradeMinorVersion;
    }
}
