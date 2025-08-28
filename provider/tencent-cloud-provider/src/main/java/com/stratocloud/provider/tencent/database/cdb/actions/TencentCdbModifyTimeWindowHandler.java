package com.stratocloud.provider.tencent.database.cdb.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.AddTimeWindowRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCdbModifyTimeWindowHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbModifyTimeWindowHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
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
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var response = provider.buildClient(account).describeCdbTimeWindow(resource.getExternalId());

        TimeWindowInput input = new TimeWindowInput();
        input.setMondayWindows(TimeWindows.fromWindows(response.getMonday()));
        input.setTuesdayWindows(TimeWindows.fromWindows(response.getTuesday()));
        input.setWednesdayWindows(TimeWindows.fromWindows(response.getWednesday()));
        input.setThursdayWindows(TimeWindows.fromWindows(response.getThursday()));
        input.setFridayWindows(TimeWindows.fromWindows(response.getFriday()));
        input.setSaturdayWindows(TimeWindows.fromWindows(response.getSaturday()));
        input.setSundayWindows(TimeWindows.fromWindows(response.getSunday()));
        input.setMaxDelayTime(response.getMaxDelayTime());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TimeWindowInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        TimeWindowInput input = JSON.convert(parameters, TimeWindowInput.class);

        AddTimeWindowRequest request = input.getAddRequest(resource.getExternalId());

        client.clearCdbTimeWindow(resource.getExternalId());
        client.addCdbTimeWindow(request);
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
        @NestedFormField(label = "星期一", nestedFormClass = TimeWindows.class)
        private TimeWindows mondayWindows;
        @NestedFormField(label = "星期二", nestedFormClass = TimeWindows.class)
        private TimeWindows tuesdayWindows;
        @NestedFormField(label = "星期三", nestedFormClass = TimeWindows.class)
        private TimeWindows wednesdayWindows;
        @NestedFormField(label = "星期四", nestedFormClass = TimeWindows.class)
        private TimeWindows thursdayWindows;
        @NestedFormField(label = "星期五", nestedFormClass = TimeWindows.class)
        private TimeWindows fridayWindows;
        @NestedFormField(label = "星期六", nestedFormClass = TimeWindows.class)
        private TimeWindows saturdayWindows;
        @NestedFormField(label = "星期日", nestedFormClass = TimeWindows.class)
        private TimeWindows sundayWindows;

        @NumberField(label = "数据延迟阈值(秒)", min = 1, max = 10, defaultValue = 10)
        private Long maxDelayTime;

        @JsonIgnore
        public AddTimeWindowRequest getAddRequest(String instanceId){
            AddTimeWindowRequest request = new AddTimeWindowRequest();
            request.setInstanceId(instanceId);
            request.setMaxDelayTime(maxDelayTime);

            if(mondayWindows != null){
                request.setMonday(mondayWindows.toWindows());
            }
            if(tuesdayWindows != null){
                request.setTuesday(tuesdayWindows.toWindows());
            }
            if(wednesdayWindows != null){
                request.setWednesday(wednesdayWindows.toWindows());
            }
            if(thursdayWindows != null){
                request.setThursday(thursdayWindows.toWindows());
            }
            if(fridayWindows != null){
                request.setFriday(fridayWindows.toWindows());
            }
            if(saturdayWindows != null){
                request.setSaturday(saturdayWindows.toWindows());
            }
            if(sundayWindows != null){
                request.setSunday(sundayWindows.toWindows());
            }
            return request;
        }
    }

    @Data
    public static class TimeWindows implements DynamicForm {
        @NestedFormField(label = "维护窗口", nestedFormClass = TimeWindow.class, multiple = true, multipleMax = 2)
        private List<TimeWindow> timeWindows;

        public static TimeWindows fromWindows(String[] windows){
            TimeWindows result = new TimeWindows();

            if(Utils.isEmpty(windows))
                return result;

            result.setTimeWindows(Arrays.stream(windows).map(TimeWindow::fromWindow).toList());

            return result;
        }

        @JsonIgnore
        public String[] toWindows() {
            if(Utils.isEmpty(timeWindows))
                return new String[0];

            return timeWindows.stream().map(TimeWindow::toWindow).toArray(String[]::new);
        }
    }

    @Data
    public static class TimeWindow implements DynamicForm{
        @DateTimeField(label = "窗口起止时间", isRange = true, timeOnly = true)
        private List<String> startTimeAndEndTime;

        @JsonIgnore
        public String toWindow(){
            return startTimeAndEndTime.get(0) + "-" + startTimeAndEndTime.get(1);
        }

        public static TimeWindow fromWindow(String window){
            String[] split = window.split("-");
            TimeWindow timeWindow = new TimeWindow();
            timeWindow.setStartTimeAndEndTime(List.of(split));
            return timeWindow;
        }
    }
}
