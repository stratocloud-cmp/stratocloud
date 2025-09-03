package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.exceptions.StratoUnsupportedException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.RedisType;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.tencentcloudapi.redis.v20180412.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentRedisBuildHandler implements BuildResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisBuildHandler(TencentRedisHandler redisHandler) {
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
        return TencentRedisBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        List<ZoneCapacityConf> zoneConfigs = provider.buildClient(account).describeRedisZoneConfigs().stream().filter(
                z -> z.getIsSaleout() == null || !z.getIsSaleout()
        ).toList();

        DynamicFormMetaData replicaNodeMetaData = RedisReplicaNode.getFormMetaData(
                zoneConfigs.stream().map(ZoneCapacityConf::getZoneId).toList(),
                zoneConfigs.stream().map(ZoneCapacityConf::getZoneName).toList()
        );

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentRedisBuildInput.class);

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData, "replicaNodes", replicaNodeMetaData
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        String instanceId = createRedisInstance(resource, parameters, false);
        resource.setExternalId(instanceId);
    }

    private String createRedisInstance(Resource resource,
                                       Map<String, Object> parameters,
                                       boolean dryRun) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentRedisBuildInput input = JSON.convert(parameters, TencentRedisBuildInput.class);

        CreateInstancesRequest request = getCreateInstancesRequest(resource, input, client, dryRun);

        return client.createRedisInstance(request);
    }

    private static CreateInstancesRequest getCreateInstancesRequest(Resource resource,
                                                                    TencentRedisBuildInput input,
                                                                    TencentCloudClient client,
                                                                    boolean dryRun) {
        CreateInstancesRequest request = new CreateInstancesRequest();

        request.setInstanceName(resource.getName());
        request.setGoodsNum(1L);
        request.setDryRun(dryRun);

        resolveBasics(request, input);
        resolvePayment(request, input);
        resolvePlacement(request, resource, client, input);
        return request;
    }

    private static void resolvePlacement(CreateInstancesRequest request,
                                         Resource resource,
                                         TencentCloudClient client,
                                         TencentRedisBuildInput input) {
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );
        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );
        List<Resource> securityGroupResources = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP);
        request.setZoneName(subnet.getZone());
        request.setVpcId(subnet.getVpcId());
        request.setSubnetId(subnet.getSubnetId());

        if(Utils.isNotEmpty(securityGroupResources))
            request.setSecurityGroupIdList(
                    securityGroupResources.stream().map(Resource::getExternalId).toList().toArray(String[]::new)
            );

        request.setRedisShardNum(
                input.getArchitecture() == RedisType.Architecture.Cluster ? input.getShardNumber() : null
        );

        if(Utils.isNotEmpty(input.getReplicaNodes())){
            request.setRedisReplicasNum((long) input.getReplicaNodes().size());
            List<RedisNodeInfo> replicaNodes = input.getReplicaNodes().stream().map(
                    n -> n.toNodeInfo(subnet.getZone())
            ).toList();
            List<RedisNodeInfo> nodes = new ArrayList<>(replicaNodes);

            RedisNodeInfo master = new RedisNodeInfo();
            master.setNodeType(0L);
            master.setZoneName(subnet.getZone());
            nodes.add(master);

            request.setNodeSet(nodes.toArray(RedisNodeInfo[]::new));
        }


        request.setReplicasReadonly(input.isReplicaReadOnly());
    }

    private static void resolvePayment(CreateInstancesRequest request, TencentRedisBuildInput input) {
        request.setBillingMode(input.getBillingMode());
        if(Objects.equals(input.getBillingMode(), 1L)){
            request.setPeriod(input.getPeriod());
            request.setAutoRenew(input.getAutoRenewFlag());
        }else {
            request.setPeriod(1L);
        }
    }

    private static void resolveBasics(CreateInstancesRequest request, TencentRedisBuildInput input) {
        String version = switch (input.getEngine()){
            case Memcached -> input.getMemcachedVersion();
            case Redis -> input.getRedisVersion();
            default -> throw new StratoUnsupportedException("Unsupported redis engine: "+ input.getEngine());
        };
        RedisType.Architecture architecture = input.getArchitecture();
        RedisType redisType = RedisType.findMatched(input.getEngine(), version, architecture);
        request.setTypeId(redisType.getId());
        request.setMemSize(
                architecture == RedisType.Architecture.Cluster ? input.getShardMemoryMb() : input.getStandardMemoryMb()
        );
        request.setVPort(input.getPort());
        request.setNoAuth(input.isNoAuth());
        request.setPassword(input.isNoAuth() ? null : input.getPassword());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        TencentRedisBuildInput input = JSON.convert(parameters, TencentRedisBuildInput.class);

        RedisType.Architecture architecture = input.getArchitecture();
        if(architecture == RedisType.Architecture.Standard){
            if(input.getStandardMemoryMb() == null)
                return List.of();

            return List.of(
                    new ResourceUsage(
                            UsageTypes.MEMORY_GB.type(),
                            BigDecimal.valueOf(input.getStandardMemoryMb()/1024.0)
                    )
            );
        }else if(architecture == RedisType.Architecture.Cluster) {
            if(input.getShardMemoryMb() == null || input.getShardNumber() == null)
                return List.of();

            return List.of(
                    new ResourceUsage(
                            UsageTypes.MEMORY_GB.type(),
                            BigDecimal.valueOf(input.getShardMemoryMb()*input.getShardNumber()/1024.0)
                    )
            );
        }else {
            return List.of();
        }
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createRedisInstance(resource, parameters, true);
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentRedisBuildInput input = JSON.convert(parameters, TencentRedisBuildInput.class);

        var createInstancesRequest = getCreateInstancesRequest(resource, input, client, true);

        InquiryPriceCreateInstanceRequest request = toInquiryPriceRequest(createInstancesRequest);

        InquiryPriceCreateInstanceResponse response = client.describeRedisPrice(request);
        Float price = response.getPrice();
        if(price == null)
            return ResourceCost.ZERO;

        double timeAmount;
        ChronoUnit timeUnit;
        if(Objects.equals(input.getBillingMode(), 1L)){
            timeAmount = input.getPeriod();
            timeUnit = ChronoUnit.MONTHS;
        }else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;
        }

        return new ResourceCost(price/100.0, timeAmount, timeUnit);
    }

    private static InquiryPriceCreateInstanceRequest toInquiryPriceRequest(CreateInstancesRequest createInstancesRequest) {
        InquiryPriceCreateInstanceRequest request = new InquiryPriceCreateInstanceRequest();
        request.setTypeId(createInstancesRequest.getTypeId());
        request.setMemSize(createInstancesRequest.getMemSize());
        request.setGoodsNum(1L);
        request.setPeriod(createInstancesRequest.getPeriod() != null ? createInstancesRequest.getPeriod() : 1L);
        request.setBillingMode(createInstancesRequest.getBillingMode());
        request.setRedisShardNum(createInstancesRequest.getRedisShardNum());
        request.setRedisReplicasNum(createInstancesRequest.getRedisReplicasNum());
        request.setReplicasReadonly(createInstancesRequest.getReplicasReadonly());
        request.setZoneName(createInstancesRequest.getZoneName());
        request.setProductVersion(createInstancesRequest.getProductVersion());
        return request;
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        if(result.taskState() == TaskState.FINISHED) {
            SleepUtil.sleep(30);
            ResourceSyncScheduler.addSyncTask(
                    new ResourceSyncScheduler.SyncTask(
                            resource.getId(),
                            30L,
                            3
                    )
            );
        }

        return result;
    }
}
