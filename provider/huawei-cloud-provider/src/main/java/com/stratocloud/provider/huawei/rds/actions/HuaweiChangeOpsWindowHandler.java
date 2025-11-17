package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.ChangeOpsWindowRequest;
import com.huaweicloud.sdk.rds.v3.model.OpsWindowRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiChangeOpsWindowHandler implements ResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiChangeOpsWindowHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.MODIFY_TIME_WINDOW;
    }

    @Override
    public String getTaskName() {
        return "更新RDS维护窗口";
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
        return ChangeInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ChangeInput input = JSON.convert(parameters, ChangeInput.class);

        ChangeOpsWindowRequest request = new ChangeOpsWindowRequest();
        request.setInstanceId(resource.getExternalId());
        request.setBody(
                new OpsWindowRequest().withStartTime(
                        TimeUtil.toUtcTime(input.getStartTime())
                ).withEndTime(
                        TimeUtil.toUtcTime(input.getEndTime())
                )
        );

        provider.buildClient(account).rds().changeOpsWindow(request);
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
    public static class ChangeInput implements ResourceActionInput {
        @SelectField(
                label = "开始时间",
                options = {
                        "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                        "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                        "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
                },
                optionNames = {
                        "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                        "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                        "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
                }
        )
        private String startTime;
        @SelectField(
                label = "结束时间",
                options = {
                        "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                        "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                        "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
                },
                optionNames = {
                        "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                        "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                        "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
                }
        )
        private String endTime;
    }
}
