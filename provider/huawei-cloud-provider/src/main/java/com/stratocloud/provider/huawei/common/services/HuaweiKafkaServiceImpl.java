package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.kafka.v2.KafkaClient;
import com.huaweicloud.sdk.kafka.v2.model.*;
import com.huaweicloud.sdk.kafka.v2.region.KafkaRegion;
import com.stratocloud.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HuaweiKafkaServiceImpl extends HuaweiAbstractService implements HuaweiKafkaService{
    public HuaweiKafkaServiceImpl(CacheService cacheService,
                                  ICredential credential,
                                  String regionId,
                                  String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private KafkaClient buildClient(){
        return KafkaClient.newBuilder()
                .withCredential(credential)
                .withRegion(KafkaRegion.valueOf(regionId))
                .build();
    }

    @Override
    public String createInstance(CreatePostPaidKafkaInstanceRequest request){
        CreatePostPaidKafkaInstanceResponse response = tryInvoke(
                () -> buildClient().createPostPaidKafkaInstance(request)
        );

        log.info("Huawei create kafka instance request sent. InstanceId={}.", response.getInstanceId());

        return response.getInstanceId();
    }

    @Override
    public List<ShowInstanceResp> describeInstances(ListInstancesRequest request){
        request.setEngine(ListInstancesRequest.EngineEnum.KAFKA);
        return queryAll(
                () -> buildClient().listInstances(request).getInstances(),
                limit -> request.setLimit(limit.toString()),
                offset -> request.setOffset(offset.toString())
        );
    }

    @Override
    public Optional<ShowInstanceResp> describeInstance(String instanceId){
        ListInstancesRequest request = new ListInstancesRequest();
        request.setInstanceId(instanceId);
        return describeInstances(request).stream().findAny();
    }

    @Override
    public void deleteInstance(String instanceId){
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);
        tryInvoke(() -> buildClient().deleteInstance(request));

        log.info("Huawei delete kafka instance request sent. InstanceId={}.", instanceId);
    }

    @Override
    public void restartInstance(String instanceId){
        BatchRestartOrDeleteInstancesRequest request = new BatchRestartOrDeleteInstancesRequest();
        BatchRestartOrDeleteInstanceReq body = new BatchRestartOrDeleteInstanceReq();
        body.setInstances(List.of(instanceId));
        body.setAction(BatchRestartOrDeleteInstanceReq.ActionEnum.RESTART);
        request.setBody(body);

        tryInvoke(() -> buildClient().batchRestartOrDeleteInstances(request));

        log.info("Huawei restart kafka instance request sent. InstanceId={}.", instanceId);
    }

    @Override
    public void updateInstance(UpdateInstanceRequest request){
        tryInvoke(() -> buildClient().updateInstance(request));

        log.info("Huawei update kafka instance request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public List<AvailableZonesResp> describeZones(){
        ListAvailableZonesRequest request = new ListAvailableZonesRequest();
        return queryAll(() -> buildClient().listAvailableZones(request).getAvailableZones());
    }

    @Override
    public List<ListEngineProductsEntity> describeProducts(){
        ListEngineProductsRequest request = new ListEngineProductsRequest();
        request.setEngine(ListEngineProductsRequest.EngineEnum.KAFKA);
        return queryAll(() -> buildClient().listEngineProducts(request).getProducts());
    }
}
