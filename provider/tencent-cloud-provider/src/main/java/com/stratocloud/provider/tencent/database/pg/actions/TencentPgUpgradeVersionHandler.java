package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.database.pg.util.PgUpgradeTargets;
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import com.tencentcloudapi.postgres.v20170312.models.UpgradeDBInstanceKernelVersionRequest;
import com.tencentcloudapi.postgres.v20170312.models.UpgradeDBInstanceMajorVersionRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentPgUpgradeVersionHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgUpgradeVersionHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
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
        PgUpgradeTargets upgradeTargets = PgUtil.getUpgradeTargets(resource);

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpgradeInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "minorUpgradeTargetKernelVersion",
                upgradeTargets.minorUpgradeTargets(),
                upgradeTargets.minorUpgradeTargets()
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "majorUpgradeTargetKernelVersion",
                upgradeTargets.majorUpgradeTargets(),
                upgradeTargets.majorUpgradeTargets()
        );

        UpgradeInput input = new UpgradeInput();
        input.setCurrentVersion(upgradeTargets.currentVersion());
        input.setBackupBeforeUpgrade(true);

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        UpgradeInput input = JSON.convert(parameters, UpgradeInput.class);

        if(input.isUpgradingMajorVersion()){
            UpgradeDBInstanceMajorVersionRequest request = new UpgradeDBInstanceMajorVersionRequest();
            request.setDBInstanceId(resource.getExternalId());
            request.setTargetDBKernelVersion(input.getMajorUpgradeTargetKernelVersion());
            request.setBackupBeforeUpgrade(input.isBackupBeforeUpgrade());
            request.setStatisticsRefreshOption(input.getStatisticsRefreshOption());
            request.setExtensionUpgradeOption(input.getExtensionUpgradeOption());

            request.setUpgradeTimeOption(input.getSwitchTag());
            if(Objects.equals(input.getSwitchTag(), 1L)){
                request.setUpgradeTimeBegin(input.getSwitchTimeRange().get(0));
                request.setUpgradeTimeEnd(input.getSwitchTimeRange().get(1));
            }

            client.upgradePgEngineMajorVersion(request);
        } else {
            UpgradeDBInstanceKernelVersionRequest request = new UpgradeDBInstanceKernelVersionRequest();

            request.setDBInstanceId(resource.getExternalId());
            request.setTargetDBKernelVersion(input.getMinorUpgradeTargetKernelVersion());
            request.setSwitchTag(input.getSwitchTag());

            if(Objects.equals(input.getSwitchTag(), 1L)){
                request.setSwitchStartTime(input.getSwitchTimeRange().get(0));
                request.setSwitchEndTime(input.getSwitchTimeRange().get(1));
            }

            client.upgradePgEngineKernelVersion(request);
        }
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<DBInstance> dbInstance = pgHandler.describePg(account, resource.getExternalId());

        if(dbInstance.isEmpty())
            return ResourceActionResult.failed("Instance cannot be found anymore");

        if(Objects.equals(dbInstance.get().getDBInstanceStatus(), "upgrading"))
            return ResourceActionResult.inProgress();

        ResourceSyncScheduler.addSyncTask(
                new ResourceSyncScheduler.SyncTask(
                        resource.getId(),
                        60L,
                        10
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
    public static class UpgradeInput implements ResourceActionInput {
        @InputField(label = "当前版本", disabled = true)
        private String currentVersion;
        @BooleanField(label = "升级大版本")
        private boolean upgradingMajorVersion;
        @SelectField(label = "目标版本", conditions = "this.upgradingMajorVersion === false")
        private String minorUpgradeTargetKernelVersion;
        @SelectField(label = "目标版本", conditions = "this.upgradingMajorVersion === true")
        private String majorUpgradeTargetKernelVersion;

        @SelectField(
                label = "切换时间",
                options = {
                        "0",
                        "1",
                        "2"
                },
                optionNames = {
                        "升级完成时",
                        "指定时间",
                        "维护时间内"
                },
                defaultValues = "0"
        )
        private Long switchTag;
        @DateTimeField(
                label = "指定切换时间范围",
                isRange = true,
                timeOnly = true,
                conditions = "this.switchTag === '1'"
        )
        private List<String> switchTimeRange;

        @BooleanField(
                label = "升级开始前是否备份",
                defaultValue = true,
                conditions = "this.upgradingMajorVersion === true"
        )
        private boolean backupBeforeUpgrade;
        @SelectField(
                label = "统计信息收集",
                options = {
                        "0",
                        "1",
                        "3"
                },
                optionNames = {
                        "不收集",
                        "升级完成前",
                        "升级完成后"
                },
                defaultValues = "0",
                conditions = "this.upgradingMajorVersion === true"
        )
        private Long statisticsRefreshOption;
        @SelectField(
                label = "插件升级设置",
                options = {
                        "0",
                        "1",
                        "2"
                },
                optionNames = {
                        "不升级插件版本",
                        "升级完成前",
                        "升级完成后"
                },
                defaultValues = "0",
                conditions = "this.upgradingMajorVersion === true"
        )
        private Long extensionUpgradeOption;
    }
}
