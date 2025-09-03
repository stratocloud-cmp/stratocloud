package com.stratocloud.provider.tencent.redis;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.redis.v20180412.models.DescribeInstancesRequest;
import com.tencentcloudapi.redis.v20180412.models.InquiryPriceCreateInstanceRequest;
import com.tencentcloudapi.redis.v20180412.models.InquiryPriceCreateInstanceResponse;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentRedisHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    private final IpAllocator ipAllocator;

    public TencentRedisHandler(TencentCloudProvider provider,
                               IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_REDIS";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云Redis实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NOSQL_DB_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<InstanceSet> redis = describeRedis(account, externalId);

        return redis.map(i -> toExternalResource(account, i));
    }

    public Optional<InstanceSet> describeRedis(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeRedisInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, InstanceSet redis) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                redis.getInstanceId(),
                redis.getInstanceName(),
                convertState(redis.getStatus())
        );
    }

    private ResourceState convertState(Long status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status.intValue()){
            case 0 -> ResourceState.BUILDING;
            case 1 -> ResourceState.CONFIGURING;
            case 2 -> ResourceState.STARTED;
            case -2, -3 -> ResourceState.SHUTDOWN;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeRedisInstances(
                new DescribeInstancesRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceSet redis = describeRedis(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Redis not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, redis));

        if(Utils.isNotBlank(redis.getWanIp())){
            RuntimeProperty ipProperty = RuntimeProperty.ofDisplayInList(
                    "ip",
                    "IP地址",
                    redis.getWanIp(),
                    redis.getWanIp()
            );
            resource.addOrUpdateRuntimeProperty(ipProperty);

            resource.getEssentialTarget(
                    ResourceCategories.SUBNET
            ).ifPresent(
                    s -> ipAllocator.forceAllocateIps(
                            s, InternetProtocol.IPv4, List.of(redis.getWanIp()), resource
                    )
            );
        }

        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port",
                "端口",
                String.valueOf(redis.getPort()),
                String.valueOf(redis.getPort())
        );
        resource.addOrUpdateRuntimeProperty(portProperty);

        RedisType redisType = RedisType.fromId(redis.getType());
        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type",
                "实例类型",
                String.valueOf(redisType.getId()),
                redisType.getLabel()
        );
        resource.addOrUpdateRuntimeProperty(typeProperty);

        RuntimeProperty memoryProperty = RuntimeProperty.ofDisplayInList(
                "memory",
                "总内存(GB)",
                "%.2f".formatted(redis.getSize()/1024),
                "%.2f".formatted(redis.getSize()/1024)
        );
        resource.addOrUpdateRuntimeProperty(memoryProperty);

        if(redis.getRedisReplicasNum() != null){
            RuntimeProperty replicaNumberProperty = RuntimeProperty.ofDisplayable(
                    "replicaNumber",
                    "副本数",
                    String.valueOf(redis.getRedisReplicasNum()),
                    String.valueOf(redis.getRedisReplicasNum())
            );
            resource.addOrUpdateRuntimeProperty(replicaNumberProperty);
        }

        if(redis.getRedisShardNum() != null){
            RuntimeProperty shardNumberProperty = RuntimeProperty.ofDisplayable(
                    "shardNumber",
                    "分片数",
                    String.valueOf(redis.getRedisShardNum()),
                    String.valueOf(redis.getRedisShardNum())
            );
            resource.addOrUpdateRuntimeProperty(shardNumberProperty);
        }

        resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(redis.getSize()/1024));


    }


    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.MEMORY_GB);
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceSet> redis = describeRedis(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceCost.ZERO;

        InquiryPriceCreateInstanceRequest request = new InquiryPriceCreateInstanceRequest();
        request.setTypeId(redis.get().getType());
        request.setMemSize(redis.get().getSize().longValue());
        request.setGoodsNum(1L);
        request.setPeriod(1L);
        request.setBillingMode(redis.get().getBillingMode());
        request.setZoneId(redis.get().getZoneId());
        request.setRedisShardNum(redis.get().getRedisShardNum());
        request.setRedisReplicasNum(redis.get().getRedisReplicasNum());
        request.setProductVersion(redis.get().getProductVersion());

        InquiryPriceCreateInstanceResponse response = client.describeRedisPrice(request);
        Float price = response.getPrice();
        if(price == null)
            return ResourceCost.ZERO;

        double timeAmount;
        ChronoUnit timeUnit;
        if(RedisUtil.isPrepaid(redis.get())){
            timeAmount = 1.0;
            timeUnit = ChronoUnit.MONTHS;
        }else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;
        }

        return new ResourceCost(price/100.0, timeAmount, timeUnit);
    }

}
