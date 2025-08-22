package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TencentCdbResetPasswordHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbResetPasswordHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
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
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        List<AccountInfo> accountInfos = client.describeCdbAccounts(resource.getExternalId());

        List<String> accounts = accountInfos.stream().map(AccountInfo::getUser).toList();

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
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ResetInput input = JSON.convert(parameters, ResetInput.class);

        AccountInfo accountInfo = client.describeCdbAccounts(resource.getExternalId()).stream().filter(
                a -> Objects.equals(a.getUser(), input.getAccountName())
        ).findAny().orElseThrow(
                () -> new StratoException("CDB account not found")
        );

        ModifyAccountPasswordRequest request = new ModifyAccountPasswordRequest();
        request.setInstanceId(resource.getExternalId());

        Account acc = new Account();
        acc.setUser(accountInfo.getUser());
        acc.setHost(accountInfo.getHost());

        request.setAccounts(new Account[]{acc});
        request.setNewPassword(input.getNewPassword());

        client.modifyCdbPassword(request);
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
