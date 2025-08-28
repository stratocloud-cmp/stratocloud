package com.stratocloud.provider.tencent.database.pg.actions;

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
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentPgUpdateHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgUpdateHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云数据库基本信息";
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

        var pg = pgHandler.describePg(account, resource.getExternalId());

        if(pg.isEmpty())
            return Optional.empty();

        UpdateInput input = new UpdateInput();
        input.setPrepaid(PgUtil.isPrepaid(pg.get()));
        input.setInstanceName(pg.get().getDBInstanceName());
        input.setAutoRenewFlag(pg.get().getAutoRenew());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        DBInstance pg = pgHandler.describePg(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("PG instance not found")
        );

        String newName = input.getInstanceName();
        if(Utils.isNotBlank(newName) && !Objects.equals(newName, pg.getDBInstanceName()))
            client.modifyPgName(pg.getDBInstanceId(), newName);

        Long autoRenewFlag = input.getAutoRenewFlag();
        if(input.isPrepaid() && autoRenewFlag != null && Objects.equals(autoRenewFlag, pg.getAutoRenew()))
            client.modifyPgAutoRenewFlag(pg.getDBInstanceId(), autoRenewFlag);
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
                        "1",
                        "2"
                },
                optionNames = {
                        "手动续费",
                        "自动续费",
                        "不续费"
                },
                defaultValues = "0",
                conditions = "this.prepaid === true"
        )
        private Long autoRenewFlag;
    }
}
