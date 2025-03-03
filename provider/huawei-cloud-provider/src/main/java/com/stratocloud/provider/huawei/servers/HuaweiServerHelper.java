package com.stratocloud.provider.huawei.servers;

import com.huaweicloud.sdk.bss.v2.model.*;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.ResourceCost;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HuaweiServerHelper {
    public static ResourceCost getPrePaidServerCost(HuaweiCloudClient client,
                                                    String inquiryId,
                                                    String flavorId,
                                                    String osSuffix,
                                                    int periodTypeNumber,
                                                    Integer periodNum,
                                                    String availabilityZone,
                                                    ChronoUnit timeUnit) {
        PeriodProductInfo productInfo = new PeriodProductInfo()
                .withId(inquiryId)
                .withCloudServiceType("hws.service.type.ec2")
                .withResourceType("hws.resource.type.vm")
                .withResourceSpec(flavorId + osSuffix)
                .withRegion(client.getRegionId())
                .withPeriodType(periodTypeNumber)
                .withPeriodNum(periodNum)
                .withSubscriptionNum(1)
                .withAvailableZone(availabilityZone);


        ListRateOnPeriodDetailRequest request = new ListRateOnPeriodDetailRequest();
        request.withBody(
                new RateOnPeriodReq().withProductInfos(
                        List.of(productInfo)
                ).withProjectId(client.getProjectId())
        );

        ListRateOnPeriodDetailResponse response = client.bss().inquiryPeriodResources(request);
        BigDecimal cost = response.getOfficialWebsiteRatingResult().getOfficialWebsiteAmount();
        return new ResourceCost(
                cost.doubleValue(),
                1.0,
                timeUnit
        );
    }

    public static ResourceCost getPostPaidServerCost(HuaweiCloudClient client,
                                                     String inquiryId,
                                                     String flavorId,
                                                     String osSuffix,
                                                     String availabilityZone) {
        DemandProductInfo productInfo = new DemandProductInfo()
                .withId(inquiryId)
                .withCloudServiceType("hws.service.type.ec2")
                .withResourceType("hws.resource.type.vm")
                .withResourceSpec(flavorId + osSuffix)
                .withRegion(client.getRegionId())
                .withUsageFactor("Duration")
                .withUsageMeasureId(4)
                .withUsageValue(BigDecimal.ONE)
                .withSubscriptionNum(1)
                .withAvailableZone(availabilityZone);


        var request = new ListOnDemandResourceRatingsRequest();
        request.withBody(
                new RateOnDemandReq().withProductInfos(
                        List.of(productInfo)
                ).withProjectId(client.getProjectId())
        );

        var response = client.bss().inquiryOnDemandResources(request);

        BigDecimal cost = response.getAmount();
        return new ResourceCost(
                cost.doubleValue(),
                1.0,
                ChronoUnit.HOURS
        );
    }
}
