package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.postgres.v20170312.models.AccountInfo;
import com.tencentcloudapi.postgres.v20170312.models.ResetAccountPasswordRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentPgResetPasswordHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgResetPasswordHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESET_PASSWORD;
    }

    @Override
    public String getTaskName() {
        return "云数据库修改密码";
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
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        var accountInfos = client.describePgAccounts(resource.getExternalId());

        List<String> accounts = accountInfos.stream().map(AccountInfo::getUserName).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResetInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "accountName",
                accounts,
                accounts
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ResetAccountPasswordRequest request = new ResetAccountPasswordRequest();
        request.setDBInstanceId(resource.getExternalId());

        request.setUserName(input.getAccountName());
        request.setPassword(input.getNewPassword());

        client.modifyPgPassword(request);
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
    public static class ResetInput implements ResourceActionInput {
        @SelectField(label = "账号")
        private String accountName;
        @InputField(label = "新密码", inputType = "password")
        private String newPassword;
    }
}
