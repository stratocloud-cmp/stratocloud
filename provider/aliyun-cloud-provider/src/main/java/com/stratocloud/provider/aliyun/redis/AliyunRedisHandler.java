package com.stratocloud.provider.aliyun.redis;

import com.aliyun.r_kvstore20150101.models.DescribeInstancesRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceAttribute;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceClass;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceFamily;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunRedisHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    private final IpAllocator ipAllocator;

    public AliyunRedisHandler(AliyunCloudProvider provider,
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
        return "ALIYUN_REDIS";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云Redis实例";
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
        Optional<RedisInstance> redis = describeRedis(account, externalId);

        return redis.map(i -> toExternalResource(account, i));
    }

    public Optional<RedisInstance> describeRedis(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClient client = provider.buildClient(account);
        return client.tair().describeInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, RedisInstance redis) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                redis.detail().getInstanceId(),
                redis.detail().getInstanceName(),
                convertState(redis.detail().getInstanceStatus())
        );
    }


    private ResourceState convertState(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status){
            case "Creating" -> ResourceState.BUILDING;
            case "Rebooting" -> ResourceState.RESTARTING;
            case "Normal" -> ResourceState.STARTED;
            case "Flushing", "Released" -> ResourceState.SHUTDOWN;
            case "Inactive" -> ResourceState.DISABLED;
            case "Unavailable" -> ResourceState.UNAVAILABLE;
            case "Error" -> ResourceState.BUILD_ERROR;
            case "Changing", "Transforming", "Migrating",
                 "BackupRecovering", "MinorVersionUpgrading",
                 "NetworkModifying", "SSLModifying", "MajorVersionUpgrading", "Updating"
                    -> ResourceState.CONFIGURING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        return client.tair().describeInstances(
                new DescribeInstancesRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        RedisInstance instance = describeRedis(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Redis instance not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, instance));

        if(Utils.isNotBlank(instance.detail().getPrivateIp()))
            resource.getEssentialTarget(ResourceCategories.SUBNET).ifPresent(
                    s -> ipAllocator.forceAllocateIps(
                            s, InternetProtocol.IPv4, List.of(instance.detail().getPrivateIp()), resource
                    )
            );

        AliyunClient client = provider.buildClient(account);

        String classCode = Utils.isNotBlank(instance.detail().getShardClass()) ?
                instance.detail().getShardClass() : instance.detail().getInstanceClass();

        Optional<RedisInstanceClass> instanceClass
                = client.tair().describeInstanceClasses(classCode).stream().findAny();

        if(instanceClass.isPresent()){
            String nodeType = instance.detail().getNodeType();
            String familySuffix = Set.of("double", "MASTER_SLAVE").contains(nodeType) ? "_HA" : "_SA";
            Optional<RedisInstanceFamily> family = Arrays.stream(RedisInstanceFamily.values()).filter(
                    f -> instanceClass.get().supportFamily(f)
            ).filter(
                    f -> f.name().endsWith(familySuffix) || f == RedisInstanceFamily.ECONOMICAL
            ).findAny();

            if(family.isPresent()){
                RuntimeProperty familyProperty = RuntimeProperty.ofDisplayInList(
                        "instanceFamily",
                        "规格类型",
                        family.get().name(),
                        family.get().getLabel()
                );
                resource.addOrUpdateRuntimeProperty(familyProperty);
            }

            RuntimeProperty classProperty = RuntimeProperty.ofDisplayInList(
                    "instanceClass",
                    "配置",
                    instanceClass.get().getRemark(),
                    instanceClass.get().getRemark()
            );
            resource.addOrUpdateRuntimeProperty(classProperty);
        }

        RedisInstanceAttribute attribute = client.tair().describeInstanceAttributes(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Redis instance attributes not found: " + resource.getExternalId())
        );

        if(attribute.detail().getShardCount() != null){
            RuntimeProperty shardCountProperty = RuntimeProperty.ofDisplayInList(
                    "shardCount",
                    "分片数",
                    String.valueOf(attribute.detail().getShardCount()),
                    String.valueOf(attribute.detail().getShardCount())
            );
            resource.addOrUpdateRuntimeProperty(shardCountProperty);
        }

        resource.updateUsageByType(
                UsageTypes.MEMORY_GB,
                new BigDecimal(
                        attribute.detail().getCapacity() / 1024
                )
        );
        resource.updateUsageByType(
                UsageTypes.DISK_GB,
                new BigDecimal(
                        attribute.detail().getStorage()
                )
        );
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.MEMORY_GB, UsageTypes.DISK_GB
        );
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        var optional = client.tair().describeInstanceAttributes(resource.getExternalId());

        if(optional.isEmpty())
            return ResourceCost.ZERO;

        var attributes = optional.get();

        DescribePriceRequest request = getDescribePriceRequest(attributes);

        DescribePriceResponseBody responseBody = client.tair().describePrice(request);

        if(responseBody.getOrder() == null)
            return ResourceCost.ZERO;

        if(Objects.equals(attributes.detail().getChargeType(), "PrePaid")){
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    attributes.getMonthPeriod(),
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

    private static DescribePriceRequest getDescribePriceRequest(RedisInstanceAttribute attributes) {
        DescribePriceRequest request = new DescribePriceRequest();

        request.setChargeType(attributes.detail().getChargeType());
        if(Objects.equals(attributes.detail().getChargeType(), "PrePaid")){
            request.setPeriod((long) attributes.getMonthPeriod());
        }

        request.setInstanceClass(attributes.detail().getInstanceClass());
        request.setZoneId(attributes.detail().getZoneId());
        request.setSecondaryZoneId(attributes.detail().getSecondaryZoneId());
        request.setEngineVersion(attributes.detail().getEngineVersion());
        request.setShardCount(attributes.detail().getShardCount());

        String nodeType = attributes.detail().getNodeType();
        if(Objects.equals(nodeType, "single")){
            nodeType = "STAND_ALONE";
        }else if(Objects.equals(nodeType, "double")){
            nodeType = "MASTER_SLAVE";
        }
        request.setNodeType(nodeType);

        String storageType = attributes.detail().getStorageType();
        String storage = attributes.detail().getStorage();
        if(Utils.isNotBlank(storageType) && Utils.isNotBlank(storage)){
            request.setInstances(
                    JSON.toJsonString(
                            List.of(
                                    Map.of(
                                            "RegionId", attributes.detail().getRegionId(),
                                            "ZoneId", attributes.detail().getZoneId(),
                                            "ShardClass", attributes.detail().getInstanceClass(),
                                            "ShardCount", attributes.detail().getShardCount(),
                                            "Capacity", storage,
                                            "StorageType", storageType
                                    )
                            )
                    )
            );
        }


        request.setOrderType("BUY");
        request.setQuantity(1L);
        return request;
    }
}
