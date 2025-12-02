package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.rocketmq.v2.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiRocketMqService {
    String createInstance(CreateInstanceByEngineRequest request);

    List<InstanceDetail> describeInstances(ListInstancesRequest request);

    Optional<InstanceDetail> describeInstance(String instanceId);

    void deleteInstance(String instanceId);

    void updateInstance(UpdateInstanceRequest request);

    List<ListAvailableZonesRespAvailableZones> describeZones();

    List<ProductEntity> describeProducts();

    Optional<ProductEntity> describeProduct(String productId);
}
