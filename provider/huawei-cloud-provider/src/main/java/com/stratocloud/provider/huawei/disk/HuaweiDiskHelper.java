package com.stratocloud.provider.huawei.disk;

import com.huaweicloud.sdk.bss.v2.model.*;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.ResourceCost;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HuaweiDiskHelper {
    public static ResourceCost getPrePaidDiskCost(HuaweiCloudClient client,
                                                  String inquiryId,
                                                  String volumeType,
                                                  int periodTypeNumber,
                                                  Integer periodNum,
                                                  Integer volumeSize,
                                                  String availabilityZone,
                                                  ChronoUnit timeUnit) {
        PeriodProductInfo productInfo = new PeriodProductInfo()
                .withId(inquiryId)
                .withCloudServiceType("hws.service.type.ebs")
                .withResourceType("hws.resource.type.volume")
                .withResourceSpec(volumeType)
                .withRegion(client.getRegionId())
                .withPeriodType(periodTypeNumber)
                .withPeriodNum(periodNum)
                .withResourceSize(volumeSize)
                .withSizeMeasureId(17)
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

    public static ResourceCost getPostPaidDiskCost(HuaweiCloudClient client,
                                                   String inquiryId,
                                                   String volumeType,
                                                   Integer volumeSize,
                                                   String availabilityZone) {
        DemandProductInfo productInfo = new DemandProductInfo()
                .withId(inquiryId)
                .withCloudServiceType("hws.service.type.ebs")
                .withResourceType("hws.resource.type.volume")
                .withResourceSpec(volumeType)
                .withRegion(client.getRegionId())
                .withUsageFactor("Duration")
                .withUsageMeasureId(4)
                .withUsageValue(BigDecimal.ONE)
                .withResourceSize(volumeSize)
                .withSizeMeasureId(17)
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
