package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.rds.v3.RdsClient;
import com.huaweicloud.sdk.rds.v3.model.*;
import com.huaweicloud.sdk.rds.v3.region.RdsRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.constants.DbEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HuaweiRdsServiceImpl extends HuaweiAbstractService implements HuaweiRdsService{
    public HuaweiRdsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private RdsClient buildClient(){
        return RdsClient.newBuilder()
                .withCredential(credential)
                .withRegion(RdsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public List<Flavor> describeFlavors(ListFlavorsRequest request){
        return queryAll(() -> buildClient().listFlavors(request).getFlavors());
    }

    @Override
    public Optional<Flavor> describeFlavor(ListFlavorsRequest.DatabaseNameEnum database, String flavorId){
        ListFlavorsRequest request = new ListFlavorsRequest();
        request.setDatabaseName(database);
        request.setSpecCode(flavorId);
        return describeFlavors(request).stream().findAny();
    }

    @Override
    public List<LDatastore> describeEngineVersions(ListDatastoresRequest request){
        return queryAll(() -> buildClient().listDatastores(request).getDataStores());
    }

    @Override
    public List<InstanceResponse> describeInstances(ListInstancesRequest request){
        return queryAll(
                () -> buildClient().listInstances(request).getInstances(),
                request::setLimit,
                request::setOffset
        );
    }

    @Override
    public Optional<InstanceResponse> describeInstance(String instanceId){
        ListInstancesRequest request = new ListInstancesRequest();
        request.setId(instanceId);
        return describeInstances(request).stream().findAny();
    }

    @Override
    public String createInstance(CreateInstanceRequest request){
        request.getBody().setRegion(regionId);

        boolean dryRun = request.getBody().getDryRun() != null ? request.getBody().getDryRun() : false;

        CreateInstanceResponse response = tryInvoke(() -> buildClient().createInstance(request));

        if (dryRun) {
            log.info("Huawei dry run create rds request accepted.");
            return null;
        } else {
            String instanceId = response.getInstance().getId();
            log.info("Huawei create rds request sent. InstanceId={}.", instanceId);
            return instanceId;
        }
    }


    @Override
    public void deleteInstance(String instanceId){
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);

        tryInvoke(() -> buildClient().deleteInstance(request));

        log.info("Huawei delete rds request sent. InstanceId={}.", instanceId);
    }

    @Override
    public void modifyInstanceName(UpdateInstanceNameRequest request){
        tryInvoke(() -> buildClient().updateInstanceName(request));

        log.info("Huawei modify rds name request sent. InstanceId={}.", request.getInstanceId());
    }



    @Override
    public String stopInstance(String instanceId){
        StopInstanceRequest request = new StopInstanceRequest();
        request.setInstanceId(instanceId);

        StopInstanceResponse response = tryInvoke(() -> buildClient().stopInstance(request));

        log.info("Huawei stop rds request sent. InstanceId={}.", request.getInstanceId());

        return response.getJobId();
    }

    @Override
    public String startInstance(String instanceId){
        StartupInstanceRequest request = new StartupInstanceRequest();
        request.setInstanceId(instanceId);

        StartupInstanceResponse response = tryInvoke(() -> buildClient().startupInstance(request));

        log.info("Huawei start rds request sent. InstanceId={}.", request.getInstanceId());

        return response.getJobId();
    }

    @Override
    public String restartInstance(StartInstanceRestartActionRequest request){
        StartInstanceRestartActionResponse response = tryInvoke(
                () -> buildClient().startInstanceRestartAction(request)
        );

        log.info("Huawei restart rds request sent. InstanceId={}.", request.getInstanceId());

        return response.getJobId();
    }

    @Override
    public void resizeInstance(StartResizeFlavorActionRequest request){
        tryInvoke(() -> buildClient().startResizeFlavorAction(request));

        log.info("Huawei resize rds request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public ListFlavorsResizeResponse describeResizeTargetFlavors(String instanceId){
        ListFlavorsResizeRequest request = new ListFlavorsResizeRequest();
        request.setInstanceId(instanceId);

        return tryInvoke(() -> buildClient().listFlavorsResize(request));
    }

    @Override
    public void enlargeVolume(StartInstanceEnlargeVolumeActionRequest request){
        tryInvoke(() -> buildClient().startInstanceEnlargeVolumeAction(request));

        log.info("Huawei enlarge rds volume request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public void deletePostPaidInstance(String instanceId){
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);

        tryInvoke(() -> buildClient().deleteInstance(request));
    }

    @Override
    public List<ConfigurationSummary> listConfigurations(DbEngine engine){
        ListConfigurationsRequest request = new ListConfigurationsRequest();
        request.setXLanguage(ListConfigurationsRequest.XLanguageEnum.ZH_CN);
        return tryInvoke(
                () -> buildClient().listConfigurations(request).getConfigurations()
        ).stream().filter(
                summary -> Objects.equals(
                        summary.getDatastoreName().getValue(), engine.name().toLowerCase()
                )
        ).toList();
    }

    @Override
    public ShowAutoEnlargePolicyResponse describeAutoEnlargePolicy(String instanceId){
        ShowAutoEnlargePolicyRequest request = new ShowAutoEnlargePolicyRequest();
        request.setInstanceId(instanceId);

        return tryInvoke(() -> buildClient().showAutoEnlargePolicy(request));
    }

    @Override
    public void setDiskAutoExpansion(SetAutoEnlargePolicyRequest request){
        tryInvoke(() -> buildClient().setAutoEnlargePolicy(request));

        log.info("Huawei set rds auto enlarge policy request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public void changeOpsWindow(ChangeOpsWindowRequest request){
        tryInvoke(() -> buildClient().changeOpsWindow(request));

        log.info("Huawei change rds ops window request sent. InstanceId={}.", request.getInstanceId());
    }

    @Override
    public Optional<ListJobInfoResponse> describeJob(String jobId){
        ListJobInfoRequest request = new ListJobInfoRequest();
        request.setId(jobId);

        return queryOne(() -> buildClient().listJobInfo(request));
    }


}
