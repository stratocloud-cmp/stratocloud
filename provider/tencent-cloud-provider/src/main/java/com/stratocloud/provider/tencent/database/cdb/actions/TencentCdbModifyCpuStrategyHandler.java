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
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.database.cdb.CdbUtil;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentCdbModifyCpuStrategyHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbModifyCpuStrategyHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.MODIFY_EXPAND_STRATEGY;
    }

    @Override
    public String getTaskName() {
        return "设置CPU弹性扩容策略";
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
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var response = provider.buildClient(account).describeCdbCpuExpandStrategy(resource.getExternalId());

        ModifyInput input = new ModifyInput();

        input.setCpuAutoExpandType(
                Utils.isBlank(response.getType()) ?
                        CpuAutoExpandType.disabled :
                        CpuAutoExpandType.valueOf(response.getType())
        );

        input.setExpandCpu(response.getExpandCpu());
        input.setAutoStrategy(AutoStrategyInput.fromAutoStrategy(response.getAutoStrategy()));
        input.setPeriodStrategy(PeriodStrategyInput.fromPeriodStrategy(response.getPeriodStrategy()));
        input.setTimeIntervalStrategy(TimeIntervalStrategyInput.fromStrategy(response.getTimeIntervalStrategy()));

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ModifyInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ModifyInput input = JSON.convert(parameters, ModifyInput.class);

        CpuAutoExpandType expandType = input.getCpuAutoExpandType();

        if(expandType == CpuAutoExpandType.disabled){
            client.stopCdbCpuExpand(resource.getExternalId());
        } else {
            StartCpuExpandRequest request = new StartCpuExpandRequest();
            request.setInstanceId(resource.getExternalId());
            request.setType(expandType.name());

            if(expandType != CpuAutoExpandType.auto)
                request.setExpandCpu(input.getExpandCpu());

            if(expandType == CpuAutoExpandType.auto)
                request.setAutoStrategy(input.getAutoStrategy().toAutoStrategy());

            if(expandType == CpuAutoExpandType.timeInterval)
                request.setTimeIntervalStrategy(input.getTimeIntervalStrategy().toStrategy());

            if(expandType == CpuAutoExpandType.period)
                request.setPeriodStrategy(input.getPeriodStrategy().toPeriodStrategy());

            try {
                client.stopCdbCpuExpand(resource.getExternalId());
            }catch (Exception e){
                log.warn(e.toString());
            }

            client.startCdbCpuExpand(request);
        }
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
    public static class ModifyInput implements ResourceActionInput {
        @SelectField(
                label = "弹性扩容类型",
                options = {
                        "auto",
                        "manual",
                        "timeInterval",
                        "period",
                        "disabled"
                },
                optionNames = {
                        "自动扩容",
                        "自定义扩容，立即生效",
                        "自定义扩容，按时间段",
                        "自定义扩容，按周期",
                        "不自动扩容"
                }
        )
        private CpuAutoExpandType cpuAutoExpandType;
        @NumberField(
                label = "扩容时增加的CPU核心数，不得超过当前实例的CPU核心数",
                conditions = {
                        "this.cpuAutoExpandType !== 'disabled'",
                        "this.cpuAutoExpandType !== 'auto'"
                }
        )
        private Long expandCpu;

        @NestedFormField(
                label = "自动扩容策略",
                nestedFormClass = AutoStrategyInput.class,
                conditions = "this.cpuAutoExpandType === 'auto'"
        )
        private AutoStrategyInput autoStrategy;
        @NestedFormField(
                label = "周期扩容策略",
                nestedFormClass = PeriodStrategyInput.class,
                conditions = "this.cpuAutoExpandType === 'period'"
        )
        private PeriodStrategyInput periodStrategy;
        @NestedFormField(
                label = "时间段扩容策略",
                nestedFormClass = TimeIntervalStrategyInput.class,
                conditions = "this.cpuAutoExpandType === 'timeInterval'"
        )
        private TimeIntervalStrategyInput timeIntervalStrategy;
    }

    @Data
    public static class AutoStrategyInput implements DynamicForm {
        @SelectField(
                label = "CPU利用率达到此值后触发扩容",
                options = {
                        "40",
                        "50",
                        "60",
                        "70",
                        "80",
                        "90"
                },
                optionNames = {
                        "40%",
                        "50%",
                        "60%",
                        "70%",
                        "80%",
                        "90%"
                },
                defaultValues = "70"
        )
        private Long expandThreshold;
        @SelectField(
                label = "弹性扩容观测周期（秒级）",
                options = {
                        "15",
                        "30",
                        "45",
                        "60",
                        "180",
                        "300",
                        "600",
                        "900",
                        "1800"
                },
                optionNames = {
                        "15",
                        "30",
                        "45",
                        "60",
                        "180",
                        "300",
                        "600",
                        "900",
                        "1800"
                },
                defaultValues = "300"
        )
        private Long expandSecondPeriod;
        @SelectField(
                label = "CPU利用率低于此值后触发缩容",
                options = {
                        "10",
                        "20",
                        "30",
                },
                optionNames = {
                        "10%",
                        "20%",
                        "30%",
                },
                defaultValues = "10"
        )
        private Long shrinkThreshold;
        @SelectField(
                label = "缩容观测周期（秒级）",
                options = {
                        "300",
                        "600",
                        "900",
                        "1800"
                },
                optionNames = {
                        "300",
                        "600",
                        "900",
                        "1800"
                },
                defaultValues = "300"
        )
        private Long shrinkSecondPeriod;

        public static AutoStrategyInput fromAutoStrategy(AutoStrategy autoStrategy){
            AutoStrategyInput input = new AutoStrategyInput();

            if(autoStrategy == null)
                return input;

            input.setExpandThreshold(autoStrategy.getExpandThreshold());
            input.setExpandSecondPeriod(autoStrategy.getExpandSecondPeriod());
            input.setShrinkThreshold(autoStrategy.getShrinkThreshold());
            input.setShrinkSecondPeriod(autoStrategy.getShrinkSecondPeriod());
            return input;
        }

        @JsonIgnore
        public AutoStrategy toAutoStrategy(){
            AutoStrategy autoStrategy = new AutoStrategy();
            autoStrategy.setExpandThreshold(expandThreshold);
            autoStrategy.setShrinkThreshold(shrinkThreshold);
            autoStrategy.setExpandSecondPeriod(expandSecondPeriod);
            autoStrategy.setShrinkSecondPeriod(shrinkSecondPeriod);
            return autoStrategy;
        }
    }

    @Data
    public static class PeriodStrategyInput implements DynamicForm {
        @BooleanField(label = "星期一", defaultValue = true)
        private boolean monday;
        @BooleanField(label = "星期二", defaultValue = true)
        private boolean tuesday;
        @BooleanField(label = "星期三", defaultValue = true)
        private boolean wednesday;
        @BooleanField(label = "星期四", defaultValue = true)
        private boolean thursday;
        @BooleanField(label = "星期五", defaultValue = true)
        private boolean friday;
        @BooleanField(label = "星期六", defaultValue = true)
        private boolean saturday;
        @BooleanField(label = "星期日", defaultValue = true)
        private boolean sunday;

        @DateTimeField(label = "时间区间", isRange = true, timeOnly = true)
        private List<String> timeInterval;

        @JsonIgnore
        public PeriodStrategy toPeriodStrategy(){
            PeriodStrategy periodStrategy = new PeriodStrategy();

            TImeCycle timeCycle = new TImeCycle();
            timeCycle.setMonday(monday);
            timeCycle.setTuesday(tuesday);
            timeCycle.setWednesday(wednesday);
            timeCycle.setThursday(thursday);
            timeCycle.setFriday(friday);
            timeCycle.setSaturday(saturday);
            timeCycle.setSunday(sunday);
            periodStrategy.setTimeCycle(timeCycle);

            TimeInterval interval = new TimeInterval();
            interval.setStartTime(timeInterval.get(0));
            interval.setEndTime(timeInterval.get(1));

            periodStrategy.setTimeInterval(interval);

            return periodStrategy;
        }

        public static PeriodStrategyInput fromPeriodStrategy(PeriodStrategy strategy){
            PeriodStrategyInput input = new PeriodStrategyInput();

            if(strategy==null || strategy.getTimeCycle() == null || strategy.getTimeInterval() == null)
                return input;

            TImeCycle timeCycle = strategy.getTimeCycle();
            TimeInterval interval = strategy.getTimeInterval();

            input.setMonday(timeCycle.getMonday());
            input.setTuesday(timeCycle.getTuesday());
            input.setWednesday(timeCycle.getWednesday());
            input.setThursday(timeCycle.getThursday());
            input.setFriday(timeCycle.getFriday());
            input.setSaturday(timeCycle.getSaturday());
            input.setSunday(timeCycle.getSunday());
            input.setTimeInterval(List.of(interval.getStartTime(), interval.getEndTime()));

            return input;
        }
    }

    @Data
    public static class TimeIntervalStrategyInput implements DynamicForm {
        @DateTimeField(label = "时间区间", isRange = true)
        private List<LocalDateTime> interval;

        public static TimeIntervalStrategyInput fromStrategy(TimeIntervalStrategy strategy){
            TimeIntervalStrategyInput input = new TimeIntervalStrategyInput();

            if(strategy == null || strategy.getStartTime() == null || strategy.getEndTime() == null)
                return input;

            input.setInterval(
                    List.of(
                            TencentTimeUtil.fromEpochSeconds(strategy.getStartTime()),
                            TencentTimeUtil.fromEpochSeconds(strategy.getEndTime())
                    )
            );

            return input;
        }

        @JsonIgnore
        public TimeIntervalStrategy toStrategy(){
            TimeIntervalStrategy strategy = new TimeIntervalStrategy();
            strategy.setStartTime(TencentTimeUtil.toEpochSeconds(interval.get(0)));
            strategy.setEndTime(TencentTimeUtil.toEpochSeconds(interval.get(1)));
            return strategy;
        }
    }

    public enum CpuAutoExpandType {
        auto,
        manual,
        timeInterval,
        period,
        disabled
    }
}
