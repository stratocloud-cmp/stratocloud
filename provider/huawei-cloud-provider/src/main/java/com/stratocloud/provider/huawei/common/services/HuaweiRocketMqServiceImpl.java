package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.rocketmq.v2.RocketMQClient;
import com.huaweicloud.sdk.rocketmq.v2.model.*;
import com.huaweicloud.sdk.rocketmq.v2.region.RocketMQRegion;
import com.stratocloud.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HuaweiRocketMqServiceImpl extends HuaweiAbstractService implements HuaweiRocketMqService{
    public HuaweiRocketMqServiceImpl(CacheService cacheService,
                                     ICredential credential,
                                     String regionId,
                                     String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private RocketMQClient buildClient(){
        return RocketMQClient.newBuilder()
                .withCredential(credential)
                .withRegion(RocketMQRegion.valueOf(regionId))
                .build();
    }

    @Override
    public String createInstance(CreateInstanceByEngineRequest request){
        request.setEngine(CreateInstanceByEngineRequest.EngineEnum.RELIABILITY);
        String instanceId = tryInvoke(() -> buildClient().createInstanceByEngine(request)).getInstanceId();
        log.info("Huawei create rocketmq request sent. InstanceId={}.", instanceId);
        return instanceId;
    }

    @Override
    public List<InstanceDetail> describeInstances(ListInstancesRequest request){
        request.setEngine(ListInstancesRequest.EngineEnum.ROCKETMQ);
        return queryAll(
                () -> buildClient().listInstances(request).getInstances(),
                request::setLimit,
                request::setOffset
        );
    }

    @Override
    public Optional<InstanceDetail> describeInstance(String instanceId){
        ListInstancesRequest request = new ListInstancesRequest();
        request.setInstanceId(instanceId);
        return describeInstances(request).stream().findAny();
    }

    @Override
    public void deleteInstance(String instanceId){
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);
        tryInvoke(() -> buildClient().deleteInstance(request));

        log.info("Huawei delete rocketmq request sent. InstanceId={}.", instanceId);
    }


    @Override
    public void updateInstance(UpdateInstanceRequest request){
        tryInvoke(() -> buildClient().updateInstance(request));

        log.info("Huawei update rocketmq instance request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public List<ListAvailableZonesRespAvailableZones> describeZones(){
        ListAvailableZonesRequest request = new ListAvailableZonesRequest();
        return queryAll(() -> buildClient().listAvailableZones(request).getAvailableZones());
    }

    @Override
    public List<ProductEntity> describeProducts(){
        ListEngineProductsRequest request = new ListEngineProductsRequest();
        request.setEngine("rocketmq");
        request.setType("advanced");
        return queryAll(
                () -> buildClient().listEngineProducts(request).getProducts(),
                request::setLimit,
                request::setOffset
        );
    }

    @Override
    public Optional<ProductEntity> describeProduct(String productId){
        return describeProducts().stream().filter(
                p -> Objects.equals(p.getProductId(), productId)
        ).findAny();
    }
}
