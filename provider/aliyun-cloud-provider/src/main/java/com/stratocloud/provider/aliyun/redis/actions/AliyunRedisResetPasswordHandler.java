package com.stratocloud.provider.aliyun.redis.actions;

import com.aliyun.r_kvstore20150101.models.DescribeAccountsResponseBody;
import com.aliyun.r_kvstore20150101.models.ResetAccountPasswordRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.services.AliyunTairService;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class AliyunRedisResetPasswordHandler implements ResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisResetPasswordHandler(AliyunRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESET_PASSWORD;
    }

    @Override
    public String getTaskName() {
        return "Redis修改密码";
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
        return ResetInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunTairService tairService = provider.buildClient(account).tair();

        DescribeAccountsResponseBody responseBody = tairService.describeAccounts(resource.getExternalId());

        if(responseBody.getAccounts() == null || Utils.isEmpty(responseBody.getAccounts().getAccount()))
            return Optional.empty();

        var accounts = responseBody.getAccounts().getAccount();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResetInput.class);

        //noinspection Convert2MethodRef
        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "accountName",
                accounts.stream().map(a -> a.getAccountName()).toList(),
                accounts.stream().map(a -> a.getAccountName()).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ResetAccountPasswordRequest request = new ResetAccountPasswordRequest();
        request.setInstanceId(resource.getExternalId());
        request.setAccountName(input.getAccountName());
        request.setAccountPassword(input.getPassword());

        client.tair().resetPassword(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = redisHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceActionResult.finished();

        if(redis.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        ResourceSyncScheduler.addSyncTask(
                new ResourceSyncScheduler.SyncTask(
                        resource.getId(),
                        20,
                        3
                )
        );

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
    public static class ResetInput implements ResourceActionInput {
        @SelectField(label = "账号")
        private String accountName;
        @InputField(label = "新密码", inputType = "password")
        private String password;
    }
}
