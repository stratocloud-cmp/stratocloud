package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.ResetAccountPasswordRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.aliyun.rds.model.RdsAccount;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
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
public class AliyunRdsResetPasswordHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsResetPasswordHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESET_PASSWORD;
    }

    @Override
    public String getTaskName() {
        return "重置RDS密码";
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
        return ResetPasswordInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<RdsInstance> rdsInstance = rdsHandler.describeRds(account, resource.getExternalId());

        if(rdsInstance.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResetPasswordInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();

        List<RdsAccount> rdsAccounts = provider.buildClient(account).rds().describeAccounts(resource.getExternalId());

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "accountName",
                rdsAccounts.stream().map(a -> a.detail().getAccountName()).toList(),
                rdsAccounts.stream().map(a -> a.detail().getAccountName()).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        ResetPasswordInput input = JSON.convert(parameters, ResetPasswordInput.class);

        ResetAccountPasswordRequest request = new ResetAccountPasswordRequest();

        request.setDBInstanceId(resource.getExternalId());
        request.setAccountName(input.getAccountName());
        request.setAccountPassword(input.getNewPassword());

        client.rds().resetAccountPassword(request);
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
    public static class ResetPasswordInput implements ResourceActionInput {
        @SelectField(label = "数据库账号")
        private String accountName;
        @InputField(label = "新密码", inputType = "password")
        private String newPassword;
    }
}
