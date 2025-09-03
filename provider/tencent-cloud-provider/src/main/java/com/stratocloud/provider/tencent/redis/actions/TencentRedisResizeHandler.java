package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.RedisType;
import com.stratocloud.provider.tencent.redis.RedisUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.redis.v20180412.models.InquiryPriceUpgradeInstanceRequest;
import com.tencentcloudapi.redis.v20180412.models.UpgradeInstanceRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentRedisResizeHandler implements ResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisResizeHandler(TencentRedisHandler redisHandler) {
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
        input.setArchitecture(RedisType.fromId(redis.get().getType()).getArchitecture());

        input.setStandardMemoryMb(redis.get().getSize().longValue());
        input.setShardMemoryMb(redis.get().getRedisShardSize());
        input.setSwitchOption(2L);

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        var redis = redisHandler.describeRedis(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Redis instance not found")
        );

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        boolean isCluster = input.getArchitecture() == RedisType.Architecture.Cluster;

        UpgradeInstanceRequest request = new UpgradeInstanceRequest();
        request.setInstanceId(resource.getExternalId());

        request.setMemSize(isCluster ? input.getShardMemoryMb() : input.getStandardMemoryMb());
        request.setRedisShardNum(redis.getRedisShardNum());
        request.setRedisReplicasNum(redis.getRedisReplicasNum());
        request.setSwitchOption(input.getSwitchOption());

        client.upgradeRedisInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = redisHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceActionResult.finished();

        if(redis.get().state() == ResourceState.CONFIGURING)
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
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();

        var redis = redisHandler.describeRedis(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceCost.ZERO;

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);
        boolean isCluster = input.getArchitecture() == RedisType.Architecture.Cluster;

        InquiryPriceUpgradeInstanceRequest request = new InquiryPriceUpgradeInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setMemSize(isCluster ? input.getShardMemoryMb() : input.getStandardMemoryMb());
        request.setRedisShardNum(redis.get().getRedisShardNum());
        request.setRedisReplicasNum(redis.get().getRedisReplicasNum());

        var response = provider.buildClient(account).describeRedisUpgradePrice(request);
        Float price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        double timeAmount;
        ChronoUnit timeUnit;

        if(RedisUtil.isPrepaid(redis.get())){
            String deadlineTime = redis.get().getDeadlineTime();
            if(Utils.isNotBlank(deadlineTime))
                timeAmount = LocalDateTime.now().until(TimeUtil.fromString(deadlineTime), ChronoUnit.MONTHS) + 1;
            else
                timeAmount = 0;

            timeUnit = ChronoUnit.MONTHS;
        }else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;
        }

        return new ResourceCost(price / 100.0, timeAmount, timeUnit);
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @SelectField(label = "架构版本", conditions = "false")
        private RedisType.Architecture architecture;

        @SelectField(
                label = "内存容量",
                options = {
                        "256", "512", "1024", "2048", "4096", "6144", "8192", "10240", "12288",
                        "16384", "20480", "24576", "32768", "40960", "49152", "65536"
                },
                optionNames = {
                        "256MB", "512MB", "1GB", "2GB", "4GB", "6GB", "8GB", "10GB", "12GB",
                        "16GB", "20GB", "24GB", "32GB", "40GB", "48GB", "64GB"
                },
                defaultValues = "4096",
                conditions = "this.architecture === 'Standard'"
        )
        private Long standardMemoryMb;

        @SelectField(
                label = "分片内存容量",
                options = {
                        "1024", "2048", "4096", "6144", "8192", "10240", "12288",
                        "16384", "20480", "24576", "32768", "40960", "49152", "65536"
                },
                optionNames = {
                        "1GB", "2GB", "4GB", "6GB", "8GB", "10GB", "12GB",
                        "16GB", "20GB", "24GB", "32GB", "40GB", "48GB", "64GB"
                },
                defaultValues = "4096",
                conditions = "this.architecture === 'Cluster'"
        )
        private Long shardMemoryMb;

        @SelectField(
                label = "切换时间",
                options = {
                        "1",
                        "2"
                },
                optionNames = {
                        "维护时间窗操作",
                        "立即操作"
                },
                defaultValues = "2"
        )
        private Long switchOption;
    }
}
