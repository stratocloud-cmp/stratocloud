package com.stratocloud.provider.aliyun.redis.actions;

import com.aliyun.r_kvstore20150101.models.DescribePriceRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceResponseBody;
import com.aliyun.r_kvstore20150101.models.ModifyInstanceSpecRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceClass;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceFamily;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunRedisResizeHandler implements ResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisResizeHandler(AliyunRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "Redis调整配置";
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
        return ResizeInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var redis = redisHandler.describeRedis(account, resource.getExternalId());

        if(redis.isEmpty())
            return Optional.empty();

        ResizeInput input = new ResizeInput();
        input.setMemoryMb(redis.get().detail().getCapacity());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        var redis = redisHandler.describeRedis(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Redis instance not found")
        );


        RedisInstanceClass instanceClass = getTargetInstanceClass(redis, client, input);

        ModifyInstanceSpecRequest request = new ModifyInstanceSpecRequest();
        request.setInstanceId(resource.getExternalId());
        request.setInstanceClass(instanceClass.getClassCode());
        request.setOrderType(input.getOrderType());
        request.setEffectiveTime(input.getEffectiveTime());

        client.tair().modifyInstanceSpec(request);
    }

    private static RedisInstanceClass getTargetInstanceClass(RedisInstance redis,
                                                             AliyunClient client,
                                                             ResizeInput input) {
        var detail = redis.detail();

        String currentClassCode = Utils.isBlank(detail.getShardClass()) ?
                detail.getInstanceClass() : detail.getShardClass();

        RedisInstanceClass currentClass = client.tair().describeInstanceClasses(
                currentClassCode
        ).stream().findAny().orElseThrow(
                () -> new StratoException("Current instance class not found")
        );

        RedisInstanceFamily family = Arrays.stream(RedisInstanceFamily.values()).filter(
                currentClass::supportFamily
        ).findAny().orElseThrow(
                () -> new StratoException("Current instance family not found")
        );

        List<RedisInstanceClass> instanceClasses = client.tair().describeInstanceClasses(
                family, detail.getZoneId(), detail.getChargeType()
        ).stream().filter(
                c -> Objects.equals(c.getCapacityMb(), input.getMemoryMb())
        ).toList();

        RedisInstanceClass instanceClass;
        if(Objects.equals(detail.getEditionType(), "Community") &&
                Objects.equals(detail.getComputingType(), "Ecs")){

            if(Objects.equals(detail.getNodeType(), "ha") && currentClassCode.contains("with.proxy")){
                instanceClass = instanceClasses.stream().filter(
                        c -> c.getClassCode().contains("with.proxy")
                ).findAny().orElseThrow();
            } else if (currentClassCode.contains(".y.")) {
                instanceClass = instanceClasses.stream().filter(
                        c -> c.getClassCode().contains(".y.")
                ).findAny().orElseThrow();
            } else {
                instanceClass = instanceClasses.stream().filter(
                        c -> !c.getClassCode().contains("with.proxy")
                ).filter(
                        c -> !c.getClassCode().contains(".y.")
                ).findAny().orElseThrow();
            }
        } else if (Objects.equals(detail.getEditionType(), "Enterprise") &&
                Objects.equals(detail.getComputingType(), "Ecs")) {
            if(Objects.equals(detail.getNodeType(), "ha") && currentClassCode.contains("with.proxy")){
                instanceClass = instanceClasses.stream().filter(
                        c -> c.getClassCode().contains("with.proxy")
                ).findAny().orElseThrow();
            } else {
                instanceClass = instanceClasses.stream().filter(
                        c -> !c.getClassCode().contains("with.proxy")
                ).findAny().orElseThrow();
            }
        } else {
            if (detail.getShardCount() == null || detail.getShardCount() == 0) {
                instanceClass = instanceClasses.stream().findAny().orElseThrow();
            } else {
                instanceClass = instanceClasses.stream().filter(
                        c -> c.supportShardNumber(Long.valueOf(detail.getShardCount()))
                ).findAny().orElseThrow(
                        () -> new BadCommandException("该规格类型下不存在此分片数量的规格")
                );
            }
        }
        return instanceClass;
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = redisHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceActionResult.finished();

        if(redis.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        ResourceSyncScheduler.addSyncTask(
                new ResourceSyncScheduler.SyncTask(
                        resource.getId(),
                        20,
                        3
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

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var attributes = client.tair().describeInstanceAttributes(resource.getExternalId());

        var redisInstance = client.tair().describeInstance(resource.getExternalId());

        if(attributes.isEmpty() || redisInstance.isEmpty())
            return ResourceCost.ZERO;

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        RedisInstanceClass targetInstanceClass = getTargetInstanceClass(redisInstance.get(), client, input);

        DescribePriceRequest request = new DescribePriceRequest();

        request.setInstanceId(resource.getExternalId());
        request.setOrderType("UPGRADE");
        request.setInstanceClass(targetInstanceClass.getClassCode());

        DescribePriceResponseBody responseBody = client.tair().describePrice(request);

        if(responseBody.getOrder() == null)
            return ResourceCost.ZERO;

        if(Objects.equals(attributes.get().detail().getChargeType(), "PrePaid")){
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    attributes.get().getMonthPeriod(),
                    ChronoUnit.MONTHS
            );
        }else {
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    1.0,
                    ChronoUnit.HOURS
            );
        }
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @SelectField(
                label = "分片内存大小",
                options = {
                        "256",
                        "1024",
                        "2048",
                        "4096",
                        "8192",
                        "16384",
                        "24576",
                        "32768",
                        "65536"
                },
                optionNames = {
                        "256MB",
                        "1GB",
                        "2GB",
                        "4GB",
                        "8GB",
                        "16GB",
                        "24GB",
                        "32GB",
                        "64GB"
                },
                defaultValues = "1024"
        )
        private Long memoryMb;

        @SelectField(
                label = "变配类型",
                options = {
                        "UPGRADE",
                        "DOWNGRADE"
                },
                optionNames = {
                        "升级配置",
                        "降级配置"
                },
                defaultValues = "UPGRADE"
        )
        private String orderType;

        @SelectField(
                label = "生效时间",
                options = {
                        "Immediately",
                        "MaintainTime"
                },
                optionNames = {
                        "立即变配",
                        "可运维时间段内变配"
                },
                defaultValues = "Immediately"
        )
        private String effectiveTime;
    }
}
