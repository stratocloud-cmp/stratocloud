package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.database.cdb.CdbUtil;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import com.tencentcloudapi.cdb.v20170320.models.UpgradeDBInstanceEngineVersionRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCdbUpgradeVersionHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbUpgradeVersionHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.UPGRADE_VERSION;
    }

    @Override
    public String getTaskName() {
        return "升级云数据库版本";
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
        return UpgradeInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceInfo> cdb = cdbHandler.describeCdb(account, resource.getExternalId());

        if(cdb.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpgradeInput.class);

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "paramList",
                CdbParamList.getFormMetaData(
                        provider.buildClient(account),
                        "HIGH_STABILITY",
                        cdb.get().getEngineType(),
                        cdb.get().getEngineVersion()
                )
        );

        UpgradeInput input = new UpgradeInput();
        input.setCurrentEngineVersion(cdb.get().getEngineVersion());
        input.setEngineVersion(CdbUtil.getSupportedUpgradeEngineVersion(cdb.get()));

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();

        UpgradeInput input = JSON.convert(parameters, UpgradeInput.class);

        UpgradeDBInstanceEngineVersionRequest request = new UpgradeDBInstanceEngineVersionRequest();
        request.setInstanceId(resource.getExternalId());
        request.setEngineVersion(input.getEngineVersion());
        request.setWaitSwitch(input.getWaitSwitch());
        request.setMaxDelayTime(input.getMaxDelayTime());

        if(Objects.equals(input.getEngineVersion(), "8.0"))
            request.setIgnoreErrKeyword(input.getIgnoreErrKeyword());
        request.setParamList(CdbParamList.getUpgradeParamInfoList(input.getParamList()));


        var response = provider.buildClient(account).upgradeCdbEngineVersion(request);
        TaskContext.setExternalTaskId(response.getAsyncRequestId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return CdbUtil.checkAsyncRequestResult(resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Data
    public static class UpgradeInput implements ResourceActionInput {
        @InputField(label = "当前MySQL数据库引擎版本", disabled = true)
        private String currentEngineVersion;
        @InputField(label = "升级后的MySQL数据库引擎版本", disabled = true)
        private String engineVersion;
        @SelectField(
                label = "切换时间",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "立刻切换",
                        "维护时间窗内"
                },
                defaultValues = "1"
        )
        private Long waitSwitch;

        @SelectField(
                label = "是否忽略关键字错误",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "不忽略",
                        "忽略"
                },
                conditions = "this.engineVersion === '8.0'"
        )
        private Long ignoreErrKeyword;

        @NumberField(label = "数据校验延迟阈值(秒)", min = 1, max = 10, defaultValue = 10)
        private Long maxDelayTime;

        @NestedFormField(label = "自定义参数", nestedFormClass = CdbParamList.class)
        private Map<String, Object> paramList;
    }
}
