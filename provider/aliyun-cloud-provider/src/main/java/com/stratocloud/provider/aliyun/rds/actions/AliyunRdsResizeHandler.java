package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceDetail;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AliyunRdsResizeHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsResizeHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "云数据库调整配置";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet().stream().filter(
                s -> s != ResourceState.SHUTDOWN
        ).collect(Collectors.toSet());
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResizeInput.class;
    }

    @SuppressWarnings("Convert2MethodRef")
    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        Optional<RdsInstanceDetail> instanceDetail = client.rds().describeInstanceDetail(resource.getExternalId());

        if(instanceDetail.isEmpty())
            return Optional.empty();

        var attributes = instanceDetail.get().attributes();

        DescribeAvailableClassesRequest request = new DescribeAvailableClassesRequest();

        request.setCategory(attributes.getCategory());
        request.setDBInstanceId(attributes.getDBInstanceId());
        request.setDBInstanceStorageType(attributes.getDBInstanceStorageType());
        request.setEngine(attributes.getEngine());
        request.setEngineVersion(attributes.getEngineVersion());
        request.setInstanceChargeType(attributes.getPayType());
        request.setOrderType("BUY");
        request.setZoneId(attributes.getZoneId());

        DescribeAvailableClassesResponseBody responseBody = client.rds().describeAvailableClasses(request);

        ResizeInput resizeInput = new ResizeInput();

        resizeInput.setClassCode(attributes.getDBInstanceClass());
        resizeInput.setStorageSize(attributes.getDBInstanceStorage());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, resizeInput);

        var availableClasses = responseBody.getDBInstanceClasses();
        if(Utils.isNotEmpty(availableClasses)){
            Map<String, RdsInstanceClass> classMap = client.rds().describeInstanceClasses().stream().collect(
                    Collectors.toMap(
                            c -> c.detail().getClassCode(),
                            c -> c
                    )
            );

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "classCode",
                    availableClasses.stream().map(c -> c.getDBInstanceClass()).toList(),
                    availableClasses.stream().map(c -> {
                        RdsInstanceClass instanceClass = classMap.get(c.getDBInstanceClass());
                        return instanceClass == null ? c.getDBInstanceClass() : instanceClass.getName();
                    }).toList()
            );
        }

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        ModifyDBInstanceSpecRequest request = new ModifyDBInstanceSpecRequest();

        request.setAutoUseCoupon(input.isAutoUseCoupon());

        request.setDBInstanceClass(input.getClassCode());
        request.setDBInstanceId(resource.getExternalId());
        request.setDBInstanceStorage(input.getStorageSize());
        request.setEffectiveTime(input.getEffectiveTime());

        if("ScheduleTime".equals(input.getEffectiveTime()))
            request.setSwitchTime(AliyunTimeUtil.toAliyunDateTime(input.getSwitchTime()));


        client.rds().modifyInstanceSpec(request);
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

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var instanceInfo = rdsHandler.describeRds(account, resource.getExternalId());

        if(instanceInfo.isEmpty())
            return ResourceCost.ZERO;

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);
        var rds = instanceInfo.get();

        DescribePriceRequest request = new DescribePriceRequest();


        double timeAmount;
        ChronoUnit timeUnit;

        if(Objects.equals(rds.detail().getPayType(), "Prepaid")){
            timeAmount = rds.getMonthPeriod();
            timeUnit = ChronoUnit.MONTHS;

            request.setCommodityCode("rds");
        } else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;

            request.setCommodityCode("bards");
        }

        request.setDBInstanceId(rds.detail().getDBInstanceId());

        request.setEngine(rds.detail().getEngine());
        request.setEngineVersion(rds.detail().getEngineVersion());

        request.setDBInstanceClass(input.getClassCode());
        request.setDBInstanceStorage(input.getStorageSize());
        request.setOrderType(input.isDowngrade() ? "DOWNGRADE" : "UPGRADE");
        request.setQuantity(1);

        var response = client.rds().describePrice(request);

        var price = response.getPriceInfo();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price.getTradePrice(), timeAmount, timeUnit);
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @BooleanField(label = "是否为降配")
        private boolean downgrade;

        @SelectField(label = "实例规格")
        private String classCode;

        @NumberField(label = "存储空间(GB)", min = 10, step = 5)
        private Integer storageSize;

        @SelectField(
                label = "生效时间",
                options = {
                        "Immediate",
                        "MaintainTime",
                        "ScheduleTime"
                },
                optionNames = {
                        "立即生效",
                        "可运维时间段内",
                        "指定时间"
                },
                defaultValues = "Immediate"
        )
        private String effectiveTime;

        @DateTimeField(label = "指定切换时间", conditions = "this.effectiveTime === 'ScheduleTime'")
        private LocalDateTime switchTime;

        @BooleanField(label = "自动使用代金券")
        private boolean autoUseCoupon;
    }
}
