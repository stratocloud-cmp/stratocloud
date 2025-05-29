package com.stratocloud.provider.huawei.common;

import com.stratocloud.provider.huawei.common.services.*;

public interface HuaweiCloudClient {
    String getRegionId();

    String getProjectId();

    void validateConnection();

    Float describeBalance();

    HuaweiEcsService ecs();

    HuaweiVpcService vpc();

    HuaweiEvsService evs();

    HuaweiImsService ims();

    HuaweiBssService bss();

    HuaweiIamService iam();

    HuaweiEipService eip();

    HuaweiKpsService kps();

    HuaweiElbService elb();

    HuaweiCocService coc();

    HuaweiCesService ces();

    HuaweiCtsService cts();
}
