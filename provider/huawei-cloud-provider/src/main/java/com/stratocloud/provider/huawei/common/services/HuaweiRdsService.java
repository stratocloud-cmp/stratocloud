package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.rds.v3.model.*;
import com.stratocloud.provider.constants.DbEngine;

import java.util.List;
import java.util.Optional;

public interface HuaweiRdsService {
    List<Flavor> describeFlavors(ListFlavorsRequest request);

    Optional<Flavor> describeFlavor(ListFlavorsRequest.DatabaseNameEnum database, String flavorId);

    List<LDatastore> describeEngineVersions(ListDatastoresRequest request);

    List<InstanceResponse> describeInstances(ListInstancesRequest request);

    Optional<InstanceResponse> describeInstance(String instanceId);

    String createInstance(CreateInstanceRequest request);

    void deleteInstance(String instanceId);

    void modifyInstanceName(UpdateInstanceNameRequest request);

    String stopInstance(String instanceId);

    String startInstance(String instanceId);

    String restartInstance(StartInstanceRestartActionRequest request);

    void resizeInstance(StartResizeFlavorActionRequest request);

    ListFlavorsResizeResponse describeResizeTargetFlavors(String instanceId);

    void enlargeVolume(StartInstanceEnlargeVolumeActionRequest request);

    void deletePostPaidInstance(String instanceId);

    List<ConfigurationSummary> listConfigurations(DbEngine engine);

    ShowAutoEnlargePolicyResponse describeAutoEnlargePolicy(String instanceId);

    void setDiskAutoExpansion(SetAutoEnlargePolicyRequest request);

    void changeOpsWindow(ChangeOpsWindowRequest request);

    Optional<ListJobInfoResponse> describeJob(String jobId);
}
