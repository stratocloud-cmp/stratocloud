package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.CustomerModifyAutoEnlargePolicyReq;
import com.huaweicloud.sdk.rds.v3.model.SetAutoEnlargePolicyRequest;
import com.huaweicloud.sdk.rds.v3.model.ShowAutoEnlargePolicyResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
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
public class HuaweiRdsSetExpansionHandler implements ResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsSetExpansionHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.MODIFY_EXPAND_STRATEGY;
    }

    @Override
    public String getTaskName() {
        return "修改RDS自动扩容策略";
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
        return ModifyInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ShowAutoEnlargePolicyResponse response
                = provider.buildClient(account).rds().describeAutoEnlargePolicy(resource.getExternalId());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ModifyInput.class);

        ModifyInput modifyInput = new ModifyInput();

        modifyInput.setSwitchOption(response.getSwitchOption());
        modifyInput.setLimitSize(response.getLimitSize());
        if(response.getTriggerThreshold() != null)
            modifyInput.setTriggerThreshold(Long.valueOf(response.getTriggerThreshold()));
        modifyInput.setStepPercent(response.getStepPercent());

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, modifyInput);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ModifyInput input = JSON.convert(parameters, ModifyInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        SetAutoEnlargePolicyRequest request = new SetAutoEnlargePolicyRequest();
        request.setInstanceId(resource.getExternalId());
        CustomerModifyAutoEnlargePolicyReq body = new CustomerModifyAutoEnlargePolicyReq();

        if(input.isSwitchOption()){
            body.setSwitchOption(true);
            body.setLimitSize(input.getLimitSize());
            body.setTriggerThreshold(
                    CustomerModifyAutoEnlargePolicyReq.TriggerThresholdEnum.fromValue(
                            input.getTriggerThreshold().intValue()
                    )
            );
            body.setStepPercent(input.getStepPercent());
        } else {
            body.setSwitchOption(false);
        }

        request.setBody(body);

        provider.buildClient(account).rds().setDiskAutoExpansion(request);
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
    public static class ModifyInput implements ResourceActionInput {
        @BooleanField(label = "是否开启存储空间自动扩容")
        private boolean switchOption;

        @NumberField(
                label = "存储空间扩容上限(GB)",
                min = 40,
                max = 4000,
                conditions = "this.switchOption === true"
        )
        private Integer limitSize;

        @SelectField(
                label = "可用存储空间百分比",
                description = "小于等于此值或者为10GB时触发扩容",
                options = {
                        "10",
                        "15",
                        "20"
                },
                optionNames = {
                        "10%",
                        "15%",
                        "20%"
                },
                defaultValues = "10",
                conditions = "this.switchOption === true"
        )
        private Long triggerThreshold;

        @NumberField(
                label = "每次自动扩容当前存储空间的百分比",
                defaultValue = 20,
                min = 5,
                max = 50,
                conditions = "this.switchOption === true"
        )
        private Integer stepPercent;
    }
}
