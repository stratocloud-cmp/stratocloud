package com.stratocloud.provider.aliyun.common;

import com.stratocloud.provider.aliyun.common.services.*;

public interface AliyunClient {

    String getRegionId();

    void validateConnection();

    Float describeBalance();

    AliyunComputeService ecs();

    AliyunNetworkService vpc();

    AliyunBillingService billing();


    AliyunClbService clb();

    AliyunCmsService cms();

}
