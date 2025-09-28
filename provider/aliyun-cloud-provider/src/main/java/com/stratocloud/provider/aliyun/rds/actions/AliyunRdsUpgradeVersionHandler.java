package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.UpgradeDBInstanceKernelVersionRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DateTimeField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class AliyunRdsUpgradeVersionHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsUpgradeVersionHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
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
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rds = provider.buildClient(account).rds().describeInstanceDetail(resource.getExternalId());

        if(rds.isEmpty())
            return Optional.empty();

        var attributes = rds.get().attributes();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpgradeInput.class);

        UpgradeInput input = new UpgradeInput();
        input.setCurrentEngineVersion("%s %s".formatted(attributes.getEngine(), attributes.getEngineVersion()));
        input.setCurrentKernelVersion(attributes.getCurrentKernelVersion());
        input.setKernelVersion("最新内核版本");

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();

        UpgradeInput input = JSON.convert(parameters, UpgradeInput.class);

        UpgradeDBInstanceKernelVersionRequest request = new UpgradeDBInstanceKernelVersionRequest();
        request.setDBInstanceId(resource.getExternalId());
        request.setUpgradeTime(input.getEffectiveTime());

        if(Objects.equals(input.getEffectiveTime(), "SpecifyTime"))
            request.setSwitchTime(AliyunTimeUtil.toAliyunDateTime(input.getSwitchTime()));

        provider.buildClient(account).rds().upgradeKernelVersion(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource rds = rdsHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("RDS instance does not exist anymore")
        );

        if(rds.state() == ResourceState.CONFIGURING)
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
    public static class UpgradeInput implements ResourceActionInput {
        @InputField(label = "当前数据库引擎版本", disabled = true)
        private String currentEngineVersion;
        @InputField(label = "当前数据库内核版本", disabled = true)
        private String currentKernelVersion;
        @InputField(label = "升级后的数据库内核版本", disabled = true)
        private String kernelVersion;
        @SelectField(
                label = "生效时间",
                options = {
                        "Immediate",
                        "MaintainTime",
                        "SpecifyTime"
                },
                optionNames = {
                        "立即生效",
                        "可运维时间段内",
                        "指定时间"
                },
                defaultValues = "Immediate"
        )
        private String effectiveTime;

        @DateTimeField(label = "指定切换时间", conditions = "this.effectiveTime === 'SpecifyTime'")
        private LocalDateTime switchTime;
    }
}
