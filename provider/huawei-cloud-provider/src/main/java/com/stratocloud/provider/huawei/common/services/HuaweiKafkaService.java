package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.kafka.v2.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiKafkaService {
    String createInstance(CreatePostPaidKafkaInstanceRequest request);

    List<ShowInstanceResp> describeInstances(ListInstancesRequest request);

    Optional<ShowInstanceResp> describeInstance(String instanceId);

    void deleteInstance(String instanceId);

    void restartInstance(String instanceId);

    void updateInstance(UpdateInstanceRequest request);

    List<AvailableZonesResp> describeZones();

    List<ListEngineProductsEntity> describeProducts();
}
