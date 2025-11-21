package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.dcs.v2.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiDcsService {
    List<FlavorsItems> describeFlavors(ListFlavorsRequest request);

    Optional<FlavorsItems> describeFlavor(String specCode);

    List<InstanceListInfo> describeInstances(ListInstancesRequest request);

    Optional<InstanceListInfo> describeInstance(String instanceId);

    String createInstance(CreateInstanceRequest request);

    void deleteInstance(String instanceId);

    void resizeInstance(ResizeInstanceRequest request);

    void restartOrFlush(RestartOrFlushInstancesRequest request);

    void resetPassword(ResetPasswordRequest request);

    void updateInstance(UpdateInstanceRequest request);
}
