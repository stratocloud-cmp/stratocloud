package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.ModifyDBInstanceMaintainTimeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.form.DateTimeField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
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
public class AliyunRdsModifyTimeWindowHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsModifyTimeWindowHandler(AliyunRdsHandler rdsHandler) {
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
        return "更新云数据库维护时间窗口";
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
        return TimeWindowInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        TimeWindowInput input = JSON.convert(parameters, TimeWindowInput.class);

        List<String> timeWindow = input.getStartTimeAndEndTime();
        if(Utils.length(timeWindow) != 2)
            throw new InvalidArgumentException(
                    "Invalid time window: "+JSON.toJsonString(timeWindow)
            );

        String startTime = TimeUtil.toUtcTime(timeWindow.get(0));
        String endTime = TimeUtil.toUtcTime(timeWindow.get(1));

        ModifyDBInstanceMaintainTimeRequest request = new ModifyDBInstanceMaintainTimeRequest();
        request.setDBInstanceId(resource.getExternalId());
        request.setMaintainTime(
                "%sZ-%sZ".formatted(startTime, endTime)
        );

        client.rds().modifyMaintainTime(request);
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
    public static class TimeWindowInput implements ResourceActionInput{
        @DateTimeField(label = "维护窗口起止时间", isRange = true, timeOnly = true)
        private List<String> startTimeAndEndTime;
    }
}
