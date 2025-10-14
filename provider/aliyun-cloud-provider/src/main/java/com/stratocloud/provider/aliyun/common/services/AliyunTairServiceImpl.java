package com.stratocloud.provider.aliyun.common.services;


import com.aliyun.r_kvstore20150101.Client;
import com.aliyun.r_kvstore20150101.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceAttribute;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceClass;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceFamily;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AliyunTairServiceImpl extends AliyunAbstractService implements AliyunTairService{
    public AliyunTairServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public List<RedisInstanceClass> describeInstanceClasses(boolean keepDuplicates){
        List<RedisInstanceClass> result = new ArrayList<>();
        Set<String> addedClassCodes = new HashSet<>();
        for (RedisInstanceFamily family : RedisInstanceFamily.values()) {
            for (RedisInstanceClass instanceClass : describeInstanceClasses(family)) {
                if(!keepDuplicates && addedClassCodes.contains(instanceClass.getClassCode()))
                    continue;

                result.add(instanceClass);
                addedClassCodes.add(instanceClass.getClassCode());
            }
        }
        return result;
    }

    @Override
    public List<RedisInstanceClass> describeInstanceClasses(String classCode){
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("TairClass-"+classCode),
                3000L,
                () ->doDescribeInstanceClass(classCode),
                new ArrayList<>()
        );
    }

    private ArrayList<RedisInstanceClass> doDescribeInstanceClass(String classCode) {
        List<RedisInstanceClass> list = describeInstanceClasses(true).stream().filter(
                c -> Objects.equals(c.getClassCode(), classCode)
        ).toList();
        return new ArrayList<>(list);
    }

    @Override
    public List<RedisInstanceClass> describeInstanceClasses(RedisInstanceFamily family) {
        DescribeAvailableResourceRequest request = new DescribeAvailableResourceRequest();
        request.setProductType(family.getProductType());
        request.setInstanceScene(family.getInstanceScene());
        List<RedisInstanceClass> classes = describeInstanceClasses(request);
        return classes.stream().filter(
                c -> c.supportFamily(family)
        ).toList();
    }

    @Override
    public List<RedisInstanceClass> describeInstanceClasses(RedisInstanceFamily family,
                                                            String zone,
                                                            String chargeType) {
        DescribeAvailableResourceRequest request = new DescribeAvailableResourceRequest();

        request.setInstanceChargeType(chargeType);
        request.setZoneId(zone);
        request.setEngine("Redis");
        request.setOrderType("BUY");
        request.setProductType(family.getProductType());
        request.setInstanceScene(family.getInstanceScene());

        List<RedisInstanceClass> classes = describeInstanceClasses(request);
        return classes.stream().filter(
                c -> c.supportFamily(family)
        ).toList();
    }


    private List<RedisInstanceClass> describeInstanceClasses(DescribeAvailableResourceRequest request) {
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("TairClasses", request),
                3000L,
                () -> doDescribeInstanceClasses(request),
                new ArrayList<>()
        );
    }

    private ArrayList<RedisInstanceClass> doDescribeInstanceClasses(DescribeAvailableResourceRequest request) {
        request.setRegionId(config.getRegionId());
        Map<String, RedisInstanceClass> resultMap = new HashMap<>();
        DescribeAvailableResourceResponseBody body = tryInvoke(
                () -> buildClient().describeAvailableResource(request)
        ).getBody();

        if(body.getAvailableZones() == null || Utils.isEmpty(body.getAvailableZones().getAvailableZone())){
            return new ArrayList<>(resultMap.values());
        }

        for (var zone : body.getAvailableZones().getAvailableZone()) {
            if(zone.getSupportedEngines() == null || Utils.isEmpty(zone.getSupportedEngines().getSupportedEngine()))
                continue;

            for (var engine : zone.getSupportedEngines().getSupportedEngine()) {
                var supportedEditionTypes = engine.getSupportedEditionTypes();
                if(supportedEditionTypes == null || Utils.isEmpty(supportedEditionTypes.getSupportedEditionType()))
                    continue;

                for (var editionType : supportedEditionTypes.getSupportedEditionType()) {
                    var seriesTypes = editionType.getSupportedSeriesTypes();

                    if(seriesTypes == null || Utils.isEmpty(seriesTypes.getSupportedSeriesType()))
                        continue;

                    for (var seriesType : seriesTypes.getSupportedSeriesType()) {
                        var supportedEngineVersions = seriesType.getSupportedEngineVersions();

                        if(supportedEngineVersions == null ||
                                Utils.isEmpty(supportedEngineVersions.getSupportedEngineVersion()))
                            continue;

                        for (var engineVersion : supportedEngineVersions.getSupportedEngineVersion()) {
                            var architectureTypes = engineVersion.getSupportedArchitectureTypes();

                            if(architectureTypes == null || Utils.isEmpty(architectureTypes.getSupportedArchitectureType()))
                                continue;

                            for (var architectureType : architectureTypes.getSupportedArchitectureType()) {
                                var shardNumbers = architectureType.getSupportedShardNumbers();

                                if(shardNumbers == null || Utils.isEmpty(shardNumbers.getSupportedShardNumber()))
                                    continue;

                                for (var shardNumber : shardNumbers.getSupportedShardNumber()) {
                                    var supportedNodeTypes = shardNumber.getSupportedNodeTypes();

                                    if(supportedNodeTypes == null ||
                                            Utils.isEmpty(supportedNodeTypes.getSupportedNodeType()))
                                        continue;

                                    for (var nodeType : supportedNodeTypes.getSupportedNodeType()) {
                                        var availableResources = nodeType.getAvailableResources();

                                        if(availableResources == null ||
                                                Utils.isEmpty(availableResources.getAvailableResource()))
                                            continue;

                                        for (var availableResource : availableResources.getAvailableResource()) {
                                            RedisInstanceClass instanceClass = resultMap.computeIfAbsent(
                                                    availableResource.getInstanceClass(),
                                                    k -> {
                                                        RedisInstanceClass c = new RedisInstanceClass();
                                                        c.setClassCode(availableResource.getInstanceClass());
                                                        c.setRemark(availableResource.getInstanceClassRemark());
                                                        c.setCapacityMb(availableResource.getCapacity());
                                                        c.setProductType(request.getProductType());
                                                        c.setInstanceScene(request.getInstanceScene());
                                                        return c;
                                                    }
                                            );

                                            RedisInstanceClass.Group group = new RedisInstanceClass.Group();
                                            group.setEngine(engine.getEngine());
                                            group.setEditionType(editionType.getEditionType());
                                            group.setSeriesType(seriesType.getSeriesType());
                                            group.setEngineVersion(engineVersion.getVersion());
                                            group.setArchitecture(architectureType.getArchitecture());
                                            group.setShardNumber(shardNumber.getShardNumber());
                                            group.setNodeType(nodeType.getSupportedNodeType());
                                            instanceClass.addGroup(group);
                                            instanceClass.addZone(zone.getZoneId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        return new ArrayList<>(resultMap.values());
    }

    @Override
    public List<RedisInstance> describeInstances(DescribeInstancesRequest request){
        request.setRegionId(config.getRegionId());
        return queryAll(
                () ->buildClient().describeInstances(request),
                resp -> resp.getBody().getInstances().getKVStoreInstance(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(
                RedisInstance::new
        ).toList();
    }

    @Override
    public Optional<RedisInstance> describeInstance(String instanceId){
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(instanceId);
        return describeInstances(request).stream().findAny();
    }

    @Override
    public Optional<RedisInstanceAttribute> describeInstanceAttributes(String instanceId){
        DescribeInstanceAttributeRequest request = new DescribeInstanceAttributeRequest();
        request.setInstanceId(instanceId);

        DescribeInstanceAttributeResponseBody responseBody = tryInvoke(
                () -> buildClient().describeInstanceAttribute(request)
        ).getBody();

        if(responseBody.getInstances() == null || Utils.isEmpty(responseBody.getInstances().getDBInstanceAttribute()))
            return Optional.empty();

        return Optional.of(
                new RedisInstanceAttribute(
                        responseBody.getInstances().getDBInstanceAttribute().get(0)
                )
        );
    }

    @Override
    public DescribePriceResponseBody describePrice(DescribePriceRequest request){
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describePrice(request)).getBody();
    }

    @Override
    public String createInstance(CreateInstanceRequest request){
        request.setRegionId(config.getRegionId());
        CreateInstanceResponse response = tryInvoke(() -> buildClient().createInstance(request));

        if(response == null)
            return null;

        CreateInstanceResponseBody body = response.getBody();

        log.info("Aliyun create redis instance request sent. InstanceId={}. RequestId={}.",
                body.getInstanceId(), body.getRequestId());

        return body.getInstanceId();
    }

    @Override
    public String createTairInstance(CreateTairInstanceRequest request) {
        request.setRegionId(config.getRegionId());
        CreateTairInstanceResponse response = tryInvoke(() -> buildClient().createTairInstance(request));

        if(response == null)
            return null;

        CreateTairInstanceResponseBody body = response.getBody();

        log.info("Aliyun create redis tair instance request sent. InstanceId={}. RequestId={}.",
                body.getInstanceId(), body.getRequestId());

        return body.getInstanceId();
    }

    @Override
    public void deleteInstance(String instanceId) {
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);

        DeleteInstanceResponseBody body = tryInvoke(() -> buildClient().deleteInstance(request)).getBody();

        log.info("Aliyun delete redis instance request sent. InstanceId={}. RequestId={}.",
                instanceId, body.getRequestId());
    }

    @Override
    public void restartInstance(RestartInstanceRequest request) {
        RestartInstanceResponseBody body = tryInvoke(() -> buildClient().restartInstance(request)).getBody();

        log.info("Aliyun restart redis instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public void renewInstance(RenewInstanceRequest request) {
        RenewInstanceResponseBody body = tryInvoke(() -> buildClient().renewInstance(request)).getBody();

        log.info("Aliyun renew redis instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public void resetPassword(ResetAccountPasswordRequest request) {
        ResetAccountPasswordResponseBody body = tryInvoke(() -> buildClient().resetAccountPassword(request)).getBody();

        log.info("Aliyun reset redis password request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public DescribeAccountsResponseBody describeAccounts(String instanceId) {
        DescribeAccountsRequest request = new DescribeAccountsRequest();
        request.setInstanceId(instanceId);

        return tryInvoke(() -> buildClient().describeAccounts(request)).getBody();
    }

    @Override
    public void modifyInstance(ModifyInstanceAttributeRequest request) {
        ModifyInstanceAttributeResponseBody body = tryInvoke(
                () -> buildClient().modifyInstanceAttribute(request)
        ).getBody();

        log.info("Aliyun modify redis instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public void modifyInstanceSpec(ModifyInstanceSpecRequest request) {
        request.setRegionId(config.getRegionId());

        ModifyInstanceSpecResponseBody body = tryInvoke(() -> buildClient().modifyInstanceSpec(request)).getBody();

        log.info("Aliyun modify redis instance spec request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }
}
