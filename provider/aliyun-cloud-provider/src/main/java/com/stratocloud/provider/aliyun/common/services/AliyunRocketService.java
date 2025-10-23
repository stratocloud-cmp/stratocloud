package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.rocketmq20220801.models.CreateInstanceRequest;
import com.aliyun.rocketmq20220801.models.GetInstanceResponseBody;
import com.aliyun.rocketmq20220801.models.ListInstancesRequest;
import com.aliyun.rocketmq20220801.models.UpdateInstanceRequest;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;

import java.util.List;
import java.util.Optional;

public interface AliyunRocketService {
    String createInstance(CreateInstanceRequest request);

    void updateInstance(String instanceId, UpdateInstanceRequest request);

    void deleteInstance(String instanceId);


    List<RocketInstance> describeInstances(ListInstancesRequest request);

    Optional<RocketInstance> describeInstance(String instanceId);

    GetInstanceResponseBody.GetInstanceResponseBodyData describeInstanceDetail(String instanceId);
}
