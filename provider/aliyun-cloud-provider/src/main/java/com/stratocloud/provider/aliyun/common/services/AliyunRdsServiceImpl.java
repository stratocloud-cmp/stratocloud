package com.stratocloud.provider.aliyun.common.services;


import com.aliyun.rds20140815.Client;
import com.aliyun.rds20140815.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.rds.model.RdsAccount;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceDetail;
import com.stratocloud.provider.constants.DbEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AliyunRdsServiceImpl extends AliyunAbstractService implements AliyunRdsService{
    public AliyunRdsServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }


    private List<RdsInstanceClass> describeInstanceClasses(String commodityCode,
                                                           DbEngine engine){
        ListClassesRequest request = new ListClassesRequest();
        request.setRegionId(config.getRegionId());
        request.setCommodityCode(commodityCode);
        request.setOrderType("BUY");
        request.setEngine(engine.name());

        var items = tryInvoke(() -> buildClient().listClasses(request)).getBody().getItems();

        if(items == null)
            return new ArrayList<>();
        return new ArrayList<>(
                items.stream().map(i -> new RdsInstanceClass(engine, i)).toList()
        );
    }

    @Override
    public List<RdsInstanceClass> describeInstanceClasses(){
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("RdsInstanceClass"),
                3000L,
                this::doDescribeInstanceClasses,
                new ArrayList<>()
        );
    }

    @Override
    public Optional<RdsInstanceClass> describeInstanceClass(String classCode){
        return describeInstanceClasses().stream().filter(
                c -> Objects.equals(c.detail().getClassCode(), classCode)
        ).findAny();
    }

    private List<RdsInstanceClass> doDescribeInstanceClasses() {
        List<RdsInstanceClass> result = new ArrayList<>();

        List<String> commodityCodes = List.of(
                "bards", "rds"
        );

        Set<String> addedClasses = new HashSet<>();

        for (String commodityCode : commodityCodes) {
            for (DbEngine engine : DbEngine.values()) {
                List<RdsInstanceClass> classes = describeInstanceClasses(commodityCode, engine);
                result.addAll(
                        classes.stream().filter(
                                c -> !addedClasses.contains(c.detail().getClassCode())
                        ).toList()
                );
                addedClasses.addAll(
                        classes.stream().map(c -> c.detail().getClassCode()).toList()
                );
            }
        }

        return result;
    }

    @Override
    public List<RdsInstance> describeInstances(DescribeDBInstancesRequest request){
        request.setRegionId(config.getRegionId());
        request.setInstanceLevel(1);
        return queryAllByToken(
                () -> buildClient().describeDBInstances(request),
                resp -> resp.getBody().getItems().getDBInstance(),
                resp -> resp.getBody().getNextToken(),
                request::setNextToken
        ).stream().map(RdsInstance::new).toList();
    }

    @Override
    public Optional<RdsInstance> describeInstance(String instanceId){
        DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
        request.setDBInstanceId(instanceId);
        Optional<RdsInstance> instance = describeInstances(request).stream().findAny();

        if(instance.isEmpty()){
            request.setDBInstanceStatus("Released");
            request.setExpired("True");
            instance = describeInstances(request).stream().findAny();
        }

        return instance;
    }

    @Override
    public Optional<RdsInstanceDetail> describeInstanceDetail(String instanceId){
        Optional<RdsInstance> instance = describeInstance(instanceId);

        if(instance.isEmpty())
            return Optional.empty();

        DescribeDBInstanceAttributeRequest attributeRequest = new DescribeDBInstanceAttributeRequest();
        attributeRequest.setDBInstanceId(instanceId);

        if(Objects.equals(instance.get().detail().getDBInstanceStatus(), "Released"))
            attributeRequest.setExpired("True");

        var attributes = tryInvoke(
                () -> buildClient().describeDBInstanceAttribute(attributeRequest)
        ).getBody().getItems().getDBInstanceAttribute().get(0);

        if(Objects.equals(instance.get().detail().getDBInstanceStatus(), "Released"))
            return Optional.of(new RdsInstanceDetail(instance.get(), attributes, List.of()));

        DescribeDBInstanceNetInfoRequest netInfoRequest = new DescribeDBInstanceNetInfoRequest();
        netInfoRequest.setDBInstanceId(instanceId);
        var netInfo = tryInvoke(
                () -> buildClient().describeDBInstanceNetInfo(netInfoRequest)
        ).getBody().getDBInstanceNetInfos().getDBInstanceNetInfo();

        netInfo = netInfo == null ? List.of() : netInfo;

        return Optional.of(new RdsInstanceDetail(instance.get(), attributes, netInfo));
    }

    @Override
    public DescribePriceResponseBody describePrice(DescribePriceRequest request){
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describePrice(request)).getBody();
    }


    @Override
    public String createInstance(CreateDBInstanceRequest request){
        if(request.getAmount()!=null && request.getAmount()>1)
            throw new StratoException("Do not create multiple instances via this api.");

        request.setRegionId(config.getRegionId());
        CreateDBInstanceResponse response = tryInvoke(() -> buildClient().createDBInstance(request));

        if(request.getDryRun() == null || !request.getDryRun()){
            log.info("Aliyun create rds instance request sent. InstanceId={}. RequestId={}.",
                    response.getBody().getDBInstanceId(), response.getBody().getRequestId());
            return response.getBody().getDBInstanceId();
        } else {
            return null;
        }
    }

    @Override
    public String createInstanceForRebuild(CreateDBInstanceForRebuildRequest request){
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().createDBInstanceForRebuild(request)).getBody();

        log.info("Aliyun rebuild rds instance request sent. OldInstanceId={}. NewInstanceId={}. RequestId={}.",
                request.getDBInstanceId(), responseBody.getDBInstanceId(), responseBody.getRequestId());

        return responseBody.getDBInstanceId();
    }

    @Override
    public void startInstance(String instanceId){
        StartDBInstanceRequest request = new StartDBInstanceRequest();
        request.setRegionId(config.getRegionId());
        request.setDBInstanceId(instanceId);
        StartDBInstanceResponseBody body = tryInvoke(() -> buildClient().startDBInstance(request)).getBody();

        log.info("Aliyun start rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void stopInstance(String instanceId){
        StopDBInstanceRequest request = new StopDBInstanceRequest();
        request.setDBInstanceId(instanceId);
        request.setRegionId(config.getRegionId());

        StopDBInstanceResponseBody body = tryInvoke(() -> buildClient().stopDBInstance(request)).getBody();

        log.info("Aliyun stop rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void deleteInstance(String instanceId, String backupKeepPolicy){
        DeleteDBInstanceRequest request = new DeleteDBInstanceRequest();
        request.setDBInstanceId(instanceId);
        request.setReleasedKeepPolicy(backupKeepPolicy);

        DeleteDBInstanceResponseBody body = tryInvoke(() -> buildClient().deleteDBInstance(request)).getBody();
        log.info("Aliyun delete rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void destroyInstance(String instanceId){
        DestroyDBInstanceRequest request = new DestroyDBInstanceRequest();
        request.setDBInstanceId(instanceId);

        DestroyDBInstanceResponseBody body = tryInvoke(() -> buildClient().destroyDBInstance(request)).getBody();
        log.info("Aliyun destroy rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void modifyInstanceName(ModifyDBInstanceDescriptionRequest request){
        var body = tryInvoke(() -> buildClient().modifyDBInstanceDescription(request)).getBody();

        log.info("Aliyun modify rds instance name request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public List<RdsAccount> describeAccounts(String instanceId){
        DescribeAccountsRequest request = new DescribeAccountsRequest();
        request.setDBInstanceId(instanceId);
        return queryAll(
                () -> buildClient().describeAccounts(request),
                resp -> resp.getBody().getAccounts().getDBInstanceAccount(),
                resp -> resp.getBody().getTotalRecordCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(RdsAccount::new).toList();
    }

    @Override
    public void resetAccountPassword(ResetAccountPasswordRequest request) {
        ResetAccountPasswordResponseBody body = tryInvoke(() -> buildClient().resetAccountPassword(request)).getBody();

        log.info("Aliyun reset rds instance password request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void modifyMaintainTime(ModifyDBInstanceMaintainTimeRequest request) {
        ModifyDBInstanceMaintainTimeResponseBody body = tryInvoke(
                () -> buildClient().modifyDBInstanceMaintainTime(request)
        ).getBody();

        log.info("Aliyun modify rds instance maintain time request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void renewInstance(RenewInstanceRequest request){
        RenewInstanceResponseBody body = tryInvoke(
                () -> buildClient().renewInstance(request)
        ).getBody();

        log.info("Aliyun renew rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public DescribeRenewalPriceResponseBody describeRenewalPrice(DescribeRenewalPriceRequest request){
        return tryInvoke(() -> buildClient().describeRenewalPrice(request)).getBody();
    }

    @Override
    public DescribeAvailableClassesResponseBody describeAvailableClasses(DescribeAvailableClassesRequest request){
        request.setRegionId(config.getRegionId());

        return tryInvoke(
                () -> buildClient().describeAvailableClasses(request)
        ).getBody();
    }

    @Override
    public void modifyInstanceSpec(ModifyDBInstanceSpecRequest request) {
        ModifyDBInstanceSpecResponseBody body = tryInvoke(() -> buildClient().modifyDBInstanceSpec(request)).getBody();

        log.info("Aliyun resize rds instance request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }

    @Override
    public void upgradeKernelVersion(UpgradeDBInstanceKernelVersionRequest request) {
        var body = tryInvoke(() -> buildClient().upgradeDBInstanceKernelVersion(request)).getBody();

        log.info("Aliyun upgrade rds instance kernel request sent. InstanceId={}. RequestId={}.",
                request.getDBInstanceId(), body.getRequestId());
    }
}
