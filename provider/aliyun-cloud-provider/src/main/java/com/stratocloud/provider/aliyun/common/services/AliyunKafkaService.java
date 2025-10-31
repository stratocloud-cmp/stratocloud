package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.alikafka20190916.models.*;
import com.stratocloud.provider.aliyun.kafka.KafkaInstance;

import java.util.List;
import java.util.Optional;

public interface AliyunKafkaService {

    String createPostPayInstance(CreatePostPayInstanceRequest request);

    String createPrePayInstance(CreatePrePayInstanceRequest request);

    List<KafkaInstance> describeInstances(GetInstanceListRequest request);

    Optional<KafkaInstance> describeInstance(String instanceId);

    void startInstance(StartInstanceRequest request);

    void releaseInstance(String instanceId);

    void deleteInstance(String instanceId);

    void modifyInstanceName(ModifyInstanceNameRequest request);
}
