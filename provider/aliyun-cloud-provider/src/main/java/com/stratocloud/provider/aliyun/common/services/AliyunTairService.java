package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.r_kvstore20150101.models.*;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceAttribute;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceClass;
import com.stratocloud.provider.aliyun.redis.model.RedisInstanceFamily;

import java.util.List;
import java.util.Optional;

public interface AliyunTairService {
    List<RedisInstanceClass> describeInstanceClasses(boolean keepDuplicates);

    List<RedisInstanceClass> describeInstanceClasses(String classCode);

    List<RedisInstanceClass> describeInstanceClasses(RedisInstanceFamily family);

    List<RedisInstanceClass> describeInstanceClasses(RedisInstanceFamily family,
                                                     String zone,
                                                     String chargeType);

    List<RedisInstance> describeInstances(DescribeInstancesRequest request);

    Optional<RedisInstance> describeInstance(String instanceId);

    Optional<RedisInstanceAttribute> describeInstanceAttributes(String instanceId);

    DescribePriceResponseBody describePrice(DescribePriceRequest request);

    String createInstance(CreateInstanceRequest request);

    String createTairInstance(CreateTairInstanceRequest request);

    void deleteInstance(String instanceId);

    void restartInstance(RestartInstanceRequest request);

    void renewInstance(RenewInstanceRequest request);

    void resetPassword(ResetAccountPasswordRequest request);

    DescribeAccountsResponseBody describeAccounts(String instanceId);

    void modifyInstance(ModifyInstanceAttributeRequest request);

    void modifyInstanceSpec(ModifyInstanceSpecRequest request);
}
