package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.bss.v2.model.ListOnDemandResourceRatingsRequest;
import com.huaweicloud.sdk.bss.v2.model.ListOnDemandResourceRatingsResponse;
import com.huaweicloud.sdk.bss.v2.model.ListRateOnPeriodDetailRequest;
import com.huaweicloud.sdk.bss.v2.model.ListRateOnPeriodDetailResponse;

public interface HuaweiBssService {
    float describeBalance();

    ListOnDemandResourceRatingsResponse inquiryOnDemandResources(ListOnDemandResourceRatingsRequest request);

    ListRateOnPeriodDetailResponse inquiryPeriodResources(ListRateOnPeriodDetailRequest request);
}
