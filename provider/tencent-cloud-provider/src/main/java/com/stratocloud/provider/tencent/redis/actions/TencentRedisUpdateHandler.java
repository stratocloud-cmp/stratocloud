package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.RedisUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentRedisUpdateHandler implements ResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisUpdateHandler(TencentRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新Redis实例基本信息";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return UpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var redis = redisHandler.describeRedis(account, resource.getExternalId());

        if(redis.isEmpty())
            return Optional.empty();

        UpdateInput input = new UpdateInput();
        input.setPrepaid(RedisUtil.isPrepaid(redis.get()));
        input.setInstanceName(redis.get().getInstanceName());
        input.setAutoRenewFlag(redis.get().getAutoRenewFlag());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        InstanceSet instance = redisHandler.describeRedis(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("PG instance not found")
        );

        String newName = input.getInstanceName();
        if(Utils.isNotBlank(newName) && !Objects.equals(newName, instance.getInstanceName()))
            client.modifyRedisInstanceName(instance.getInstanceId(), newName);

        Long autoRenewFlag = input.getAutoRenewFlag();
        if(input.isPrepaid() && autoRenewFlag != null && !Objects.equals(autoRenewFlag, instance.getAutoRenewFlag()))
            client.modifyRedisAutoRenewFlag(instance.getInstanceId(), autoRenewFlag);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
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
    public static class UpdateInput implements ResourceActionInput {
        @BooleanField(label = "是否预付费", conditions = "false")
        private boolean prepaid;
        @InputField(label = "实例名称")
        private String instanceName;
        @SelectField(
                label = "是否自动续费",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "手动续费",
                        "自动续费"
                },
                defaultValues = "0",
                conditions = "this.prepaid === true"
        )
        private Long autoRenewFlag;
    }
}
