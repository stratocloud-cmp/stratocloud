package com.stratocloud.provider.aliyun.redis.actions;

import com.aliyun.r_kvstore20150101.models.CreateInstanceRequest;
import com.aliyun.r_kvstore20150101.models.CreateTairInstanceRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceClass;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceFamily;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.zone.AliyunZone;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunRedisBuildHandler implements BuildResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisBuildHandler(AliyunRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Redis实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunRedisBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();

        List<AliyunZone> zones = provider.buildClient(account).ecs().describeZones();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunRedisBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "slaveZone",
                zones.stream().map(AliyunZone::getZoneId).toList(),
                zones.stream().map(z -> z.zone().getLocalName()).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        String instanceId = createRedisInstance(resource, parameters, false);
        resource.setExternalId(instanceId);
    }

    private String createRedisInstance(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        AliyunRedisBuildInput input = JSON.convert(parameters, AliyunRedisBuildInput.class);

        RedisInstanceFamily family = RedisInstanceFamily.from(input).orElseThrow(
                () -> new BadCommandException("不存在此类实例规格")
        );

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        AliyunSubnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        List<RedisInstanceClass> instanceClasses = client.tair().describeInstanceClasses(
                family, subnet.detail().getZoneId(), input.getChargeType()
        );

        if(instanceClasses.isEmpty())
            throw new BadCommandException("该可用区下不提供此规格类型");

        List<RedisInstanceClass> capacityMatchedClasses = instanceClasses.stream().filter(
                c -> Objects.equals(c.getCapacityMb(), input.getMemoryMb())
        ).toList();

        if(capacityMatchedClasses.isEmpty())
            throw new BadCommandException("该规格类型下不存在此内存容量的规格");


        String instanceId;

        if(Objects.equals(input.getEdition(), "Enterprise") &&
                !Objects.equals(input.getEnterpriseProductType(), "Local")){
            CreateTairInstanceRequest request = getCreateTairInstanceRequest(
                    resource, dryRun, input, capacityMatchedClasses, subnet
            );

            instanceId = client.tair().createTairInstance(request);
        } else {
            CreateInstanceRequest request = getCreateInstanceRequest(
                    resource, dryRun, input, capacityMatchedClasses, subnet
            );

            instanceId = client.tair().createInstance(request);
        }
        return instanceId;
    }

    private static CreateInstanceRequest getCreateInstanceRequest(Resource resource,
                                                                  boolean dryRun,
                                                                  AliyunRedisBuildInput input,
                                                                  List<RedisInstanceClass> capacityMatchedClasses,
                                                                  AliyunSubnet subnet) {
        RedisInstanceClass instanceClass;

        CreateInstanceRequest request = new CreateInstanceRequest();

        if(Objects.equals(input.getEdition(), "Community") &&
                Objects.equals(input.getCommunityProductType(), "OnECS")){

            if(Objects.equals(input.getNodeType(), "ha") && input.isEnableReadOnlyReplica()){
                instanceClass = capacityMatchedClasses.stream().filter(
                        c -> c.getClassCode().contains("with.proxy")
                ).findAny().orElseThrow();
            } else if (Objects.equals(input.getScene(), "economical")) {
                instanceClass = capacityMatchedClasses.stream().filter(
                        c -> c.getClassCode().contains(".y.")
                ).findAny().orElseThrow();
            } else {
                instanceClass = capacityMatchedClasses.stream().filter(
                        c -> !c.getClassCode().contains("with.proxy")
                ).filter(
                        c -> !c.getClassCode().contains(".y.")
                ).findAny().orElseThrow();
            }

            request.setEngineVersion(input.getCloudNativeEngineVersion());
        } else {
            if (Objects.equals(input.getArchitecture(), "standard")) {
                instanceClass = capacityMatchedClasses.stream().findAny().orElseThrow();
            } else {
                instanceClass = capacityMatchedClasses.stream().filter(
                        c -> c.supportShardNumber(input.getClassicShardNumber())
                ).findAny().orElseThrow(
                        () -> new BadCommandException("该规格类型下不存在此分片数量的规格")
                );
            }

            request.setEngineVersion(input.getClassicEngineVersion());
        }

        request.setInstanceClass(instanceClass.getClassCode());
        request.setInstanceName(resource.getName());
        request.setAppendonly(input.getAppendonly());
        request.setInstanceType("Redis");
        request.setNetworkType("VPC");
        request.setPassword(input.getPassword());
        request.setPort(String.valueOf(input.getPort()));
        request.setDryRun(dryRun);

        request.setChargeType(input.getChargeType());
        if(Objects.equals(input.getChargeType(), "PrePaid")){
            request.setPeriod(input.getPrepaidConfig().getPeriod().toString());

            if(input.getPrepaidConfig().isAutoRenew()){
                request.setAutoRenew("true");
                request.setAutoRenewPeriod(input.getPrepaidConfig().getAutoRenewPeriod());
            }
        }
        request.setAutoUseCoupon(String.valueOf(input.isAutoUseCoupon()));

        request.setVSwitchId(subnet.detail().getVSwitchId());
        request.setVpcId(subnet.detail().getVpcId());
        request.setZoneId(subnet.detail().getZoneId());

        if(Objects.equals(input.getZoneType(), "double"))
            request.setSecondaryZoneId(input.getSlaveZone());


        if(Objects.equals(input.getEdition(), "Community") &&
                Objects.equals(input.getCommunityProductType(), "OnECS")){
            if(Objects.equals(input.getNodeType(), "ha")) {
                request.setNodeType("MASTER_SLAVE");

                if(input.isEnableReadOnlyReplica()){
                    request.setReadOnlyCount(input.getMasterZoneReplicaNumber().intValue()-1);

                    if(Objects.equals(input.getZoneType(), "double"))
                        request.setSlaveReadOnlyCount(input.getSlaveZoneReplicaNumber().intValue());
                }else {
                    request.setReplicaCount(input.getMasterZoneReplicaNumber().intValue()-1);

                    if(Objects.equals(input.getZoneType(), "double"))
                        request.setSlaveReplicaCount(input.getSlaveZoneReplicaNumber().intValue());
                }
            } else {
                request.setNodeType("STAND_ALONE");
            }

            if(Objects.equals(input.getArchitecture(), "cluster"))
                request.setShardCount(input.getCloudNativeShardNumber().intValue());
        }else {
            if(Objects.equals(input.getNodeType(), "ha"))
                request.setNodeType("double");
            else
                request.setNodeType("single");
        }
        return request;
    }

    private static CreateTairInstanceRequest getCreateTairInstanceRequest(Resource resource,
                                                                          boolean dryRun,
                                                                          AliyunRedisBuildInput input,
                                                                          List<RedisInstanceClass> capacityMatchedClasses,
                                                                          AliyunSubnet subnet) {
        String productType = input.getEnterpriseProductType();

        if(Objects.equals(productType, "Tair_rdb")){
            capacityMatchedClasses = capacityMatchedClasses.stream().filter(
                    c -> c.getClassCode().contains(".rdb.")
            ).toList();
        }else if(Objects.equals(productType, "Tair_scm")){
            capacityMatchedClasses = capacityMatchedClasses.stream().filter(
                    c -> c.getClassCode().contains(".scm.")
            ).toList();
        } else if (Objects.equals(productType, "Tair_essd")) {
            capacityMatchedClasses = capacityMatchedClasses.stream().filter(
                    c -> c.getClassCode().contains(".essd.")
            ).toList();
        }

        RedisInstanceClass instanceClass;

        if(Objects.equals(input.getNodeType(), "ha") && input.isEnableReadOnlyReplica()){
            instanceClass = capacityMatchedClasses.stream().filter(
                    c -> c.getClassCode().contains("with.proxy")
            ).findAny().orElseThrow();
        } else {
            instanceClass = capacityMatchedClasses.stream().filter(
                    c -> !c.getClassCode().contains("with.proxy")
            ).findAny().orElseThrow();
        }

        CreateTairInstanceRequest request = new CreateTairInstanceRequest();

        request.setAutoPay(true);
        request.setChargeType(input.getChargeType());
        if(Objects.equals(input.getChargeType(), "PrePaid")){
            request.setPeriod(input.getPrepaidConfig().getPeriod().intValue());

            if(input.getPrepaidConfig().isAutoRenew()){
                request.setAutoRenew("true");
                request.setAutoRenewPeriod(input.getPrepaidConfig().getAutoRenewPeriod());
            }
        }
        request.setAutoUseCoupon(String.valueOf(input.isAutoUseCoupon()));


        if(Objects.equals(productType, "Tair_rdb")){
            request.setEngineVersion(input.getCloudNativeEngineVersion());
        }else if(Objects.equals(productType, "Tair_scm")){
            request.setEngineVersion("1.0");
        } else if (Objects.equals(productType, "Tair_essd")) {
            request.setEngineVersion("1.0");

            request.setStorage(input.getStorageGb());
            request.setStorageType(input.getStorageType());
        }

        if(Objects.equals(input.getArchitecture(), "cluster")){
            request.setShardCount(input.getCloudNativeShardNumber().intValue());
        }

        if(Objects.equals(input.getNodeType(), "ha")){
            request.setShardType("MASTER_SLAVE");
            if(input.isEnableReadOnlyReplica()){
                request.setReadOnlyCount(input.getMasterZoneReplicaNumber().intValue()-1);
                if(Objects.equals(input.getZoneType(), "double"))
                    request.setSlaveReadOnlyCount(input.getSlaveZoneReplicaNumber().intValue());
            }else {
                request.setReplicaCount(input.getMasterZoneReplicaNumber().intValue()-1);
                if(Objects.equals(input.getZoneType(), "double"))
                    request.setSlaveReplicaCount(input.getSlaveZoneReplicaNumber().intValue());
            }
        }else {
            request.setShardType("STAND_ALONE");
        }

        if(Objects.equals(input.getZoneType(), "double"))
            request.setSecondaryZoneId(input.getSlaveZone());


        request.setInstanceClass(instanceClass.getClassCode());
        request.setInstanceName(resource.getName());
        request.setInstanceType(productType.toLowerCase());
        request.setPassword(input.getPassword());
        request.setPort(input.getPort());
        request.setDryRun(dryRun);


        request.setVSwitchId(subnet.detail().getVSwitchId());
        request.setVpcId(subnet.detail().getVpcId());
        request.setZoneId(subnet.detail().getZoneId());
        return request;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createRedisInstance(resource, parameters, true);
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        AliyunRedisBuildInput input = JSON.convert(parameters, AliyunRedisBuildInput.class);

        RedisInstanceFamily family = RedisInstanceFamily.from(input).orElseThrow(
                () -> new BadCommandException("不存在此类实例规格")
        );

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        AliyunSubnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        List<RedisInstanceClass> instanceClasses = client.tair().describeInstanceClasses(
                family, subnet.detail().getZoneId(), input.getChargeType()
        );

        if(instanceClasses.isEmpty())
            return ResourceCost.ZERO;

        List<RedisInstanceClass> capacityMatchedClasses = instanceClasses.stream().filter(
                c -> Objects.equals(c.getCapacityMb(), input.getMemoryMb())
        ).toList();

        if(capacityMatchedClasses.isEmpty())
            throw new BadCommandException("该规格类型下不存在此内存容量的规格");

        DescribePriceRequest request = new DescribePriceRequest();

        request.setChargeType(input.getChargeType());
        request.setOrderType("BUY");
        if(Objects.equals(input.getChargeType(), "PrePaid")){
            request.setPeriod(input.getPrepaidConfig().getPeriod());
        }

        request.setZoneId(subnet.detail().getZoneId());

        if(Objects.equals(input.getEdition(), "Enterprise") &&
                !Objects.equals(input.getEnterpriseProductType(), "Local")){
            CreateTairInstanceRequest createRequest = getCreateTairInstanceRequest(
                    resource, true, input, capacityMatchedClasses, subnet
            );

            request.setSecondaryZoneId(createRequest.getSecondaryZoneId());

            request.setInstanceClass(createRequest.getInstanceClass());

            request.setNodeType(createRequest.getShardType());

            request.setShardCount(createRequest.getShardCount());

            if(Objects.equals(input.getNodeType(), "ha") && input.isEnableReadOnlyReplica()){
                int readOnlyCount = createRequest.getReadOnlyCount();

                if(createRequest.getSlaveReadOnlyCount() != null)
                    readOnlyCount = readOnlyCount + createRequest.getSlaveReadOnlyCount();

                Map<String, String> instance = Map.of(
                        "RegionId", client.getRegionId(),
                        "ShardClass", createRequest.getInstanceClass(),
                        "ZoneId", createRequest.getZoneId(),
                        "ReadOnlyCount", String.valueOf(readOnlyCount),
                        "ShardCount", createRequest.getShardCount() != null ? String.valueOf(createRequest.getShardCount()) : "1"
                );
                request.setInstances(
                        JSON.toJsonString(List.of(instance))
                );
            }else if(createRequest.getStorage() != null){
                Map<String, String> instance = Map.of(
                        "RegionId", client.getRegionId(),
                        "ShardClass", createRequest.getInstanceClass(),
                        "ZoneId", createRequest.getZoneId(),
                        "Capacity", String.valueOf(createRequest.getStorage() * 1024),
                        "StorageType", createRequest.getStorageType(),
                        "ShardCount", "1"
                );
                request.setInstances(
                        JSON.toJsonString(List.of(instance))
                );
            }
        } else {
            CreateInstanceRequest createRequest = getCreateInstanceRequest(
                    resource, true, input, capacityMatchedClasses, subnet
            );

            request.setSecondaryZoneId(createRequest.getSecondaryZoneId());

            request.setInstanceClass(createRequest.getInstanceClass());

            if(Objects.equals(input.getNodeType(), "ha"))
                request.setNodeType("MASTER_SLAVE");
            else
                request.setNodeType("STAND_ALONE");

            request.setShardCount(createRequest.getShardCount());

            if(Objects.equals(input.getEdition(), "Community") &&
                    !Objects.equals(input.getEnterpriseProductType(), "Local")){
                if(Objects.equals(input.getNodeType(), "ha") && input.isEnableReadOnlyReplica()){
                    int readOnlyCount = createRequest.getReadOnlyCount();

                    if(createRequest.getSlaveReadOnlyCount() != null)
                        readOnlyCount = readOnlyCount + createRequest.getSlaveReadOnlyCount();

                    Map<String, String> instance = Map.of(
                            "RegionId", client.getRegionId(),
                            "ShardClass", createRequest.getInstanceClass(),
                            "ZoneId", createRequest.getZoneId(),
                            "ReadOnlyCount", String.valueOf(readOnlyCount)
                    );
                    request.setInstances(
                            JSON.toJsonString(List.of(instance))
                    );
                }
            }
        }


        DescribePriceResponseBody responseBody = client.tair().describePrice(request);

        if(responseBody.getOrder() == null)
            return ResourceCost.ZERO;

        if(Objects.equals(input.getChargeType(), "PrePaid")){
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    input.getPrepaidConfig().getPeriod(),
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
}
