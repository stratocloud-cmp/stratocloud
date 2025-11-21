package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.dcs.v2.DcsClient;
import com.huaweicloud.sdk.dcs.v2.model.*;
import com.huaweicloud.sdk.dcs.v2.region.DcsRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HuaweiDcsServiceImpl extends HuaweiAbstractService implements HuaweiDcsService{
    public HuaweiDcsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private DcsClient buildClient(){
        return DcsClient.newBuilder()
                .withCredential(credential)
                .withRegion(DcsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public List<FlavorsItems> describeFlavors(ListFlavorsRequest request){
        return queryAll(
                () -> buildClient().listFlavors(request).getFlavors()
        );
    }
    @Override
    public Optional<FlavorsItems> describeFlavor(String specCode){
        ListFlavorsRequest request = new ListFlavorsRequest();
        request.setSpecCode(specCode);
        return describeFlavors(request).stream().findAny();
    }

    @Override
    public List<InstanceListInfo> describeInstances(ListInstancesRequest request){
        return queryAll(
                () -> buildClient().listInstances(request).getInstances(),
                request::setLimit,
                request::setOffset
        );
    }

    @Override
    public Optional<InstanceListInfo> describeInstance(String instanceId){
        ListInstancesRequest request = new ListInstancesRequest();
        request.setInstanceId(instanceId);
        return describeInstances(request).stream().findAny();
    }

    @Override
    public String createInstance(CreateInstanceRequest request){
        if(request.getBody().getInstanceNum() != null && request.getBody().getInstanceNum() > 1)
            throw new StratoException("Do not create multiple instances using this method");

        CreateInstanceResponse response = tryInvoke(() -> buildClient().createInstance(request));

        String instanceId = response.getInstances().get(0).getInstanceId();

        log.info("Huawei create dcs instance request sent. InstanceId={}.", instanceId);

        return instanceId;
    }

    @Override
    public void deleteInstance(String instanceId){
        DeleteSingleInstanceRequest request = new DeleteSingleInstanceRequest();
        request.setInstanceId(instanceId);
        tryInvoke(() -> buildClient().deleteSingleInstance(request));

        log.info("Huawei delete dcs instance request sent. InstanceId={}.", instanceId);
    }

    @Override
    public void resizeInstance(ResizeInstanceRequest request){
        tryInvoke(() -> buildClient().resizeInstance(request));

        log.info("Huawei resize dcs instance request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public void restartOrFlush(RestartOrFlushInstancesRequest request){
        if(Utils.length(request.getBody().getInstances()) != 1)
            throw new StratoException("Do not operate zero or multiple instances using this method");

        var response = tryInvoke(() -> buildClient().restartOrFlushInstances(request));

        String result = response.getResults().get(0).getResult();

        if(!Objects.equals(result, "success"))
            throw new StratoException("Failed to %s dcs instance".formatted(request.getBody().getAction()));

        log.info("Huawei {} dcs instance request sent. InstanceId={}.",
                request.getBody().getAction(), request.getBody().getInstances().get(0));
    }

    @Override
    public void resetPassword(ResetPasswordRequest request){
        tryInvoke(() -> buildClient().resetPassword(request));

        log.info("Huawei reset dcs instance password request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public void updateInstance(UpdateInstanceRequest request){
        tryInvoke(() -> buildClient().updateInstance(request));

        log.info("Huawei update dcs instance request sent. InstanceId={}.", request.getInstanceId());
    }
}
