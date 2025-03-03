package com.stratocloud.provider.huawei.eip.actions;

import com.huaweicloud.sdk.bss.v2.model.*;
import com.huaweicloud.sdk.eip.v2.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.eip.HuaweiEipHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class HuaweiEipBuildHandler implements BuildResourceActionHandler {

    private final HuaweiEipHandler eipHandler;

    public HuaweiEipBuildHandler(HuaweiEipHandler eipHandler) {
        this.eipHandler = eipHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return eipHandler;
    }

    @Override
    public String getTaskName() {
        return "创建弹性IP";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiEipBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, HuaweiEipBuildInput.class);

        HuaweiEipShareType shareType = input.getShareType();
        HuaweiEipChargeMode chargeMode = input.getChargeMode();

        CreatePublicipBandwidthOption bandwidthOption = new CreatePublicipBandwidthOption();

        if(shareType == HuaweiEipShareType.WHOLE){
            bandwidthOption.setShareType(CreatePublicipBandwidthOption.ShareTypeEnum.WHOLE);
            bandwidthOption.setId(input.getBandwidthId());
        }else {
            bandwidthOption.setShareType(CreatePublicipBandwidthOption.ShareTypeEnum.PER);
            bandwidthOption.setSize(input.getBandwidthSize());
            bandwidthOption.setName(resource.getName());
        }

        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        if(chargeMode == HuaweiEipChargeMode.PREPAID_BY_BANDWIDTH){
            bandwidthOption.setChargeMode(CreatePublicipBandwidthOption.ChargeModeEnum.BANDWIDTH);

            CreatePrePaidPublicipOption option = new CreatePrePaidPublicipOption();
            option.setIpVersion(
                    input.getIpVersion() == InternetProtocol.IPv6 ?
                            CreatePrePaidPublicipOption.IpVersionEnum.NUMBER_6:
                            CreatePrePaidPublicipOption.IpVersionEnum.NUMBER_4
            );
            option.setType(input.getEipType());
            option.setAlias(resource.getName());

            var extendParamOption = getExtendParamOption(input);


            String eipId = provider.buildClient(account).eip().createPrePaidEip(
                    new CreatePrePaidPublicipRequest().withBody(
                            new CreatePrePaidPublicipRequestBody().withBandwidth(bandwidthOption).withPublicip(
                                    option
                            ).withExtendParam(extendParamOption)
                    )
            );
            resource.setExternalId(eipId);
        }else {
            if(chargeMode == HuaweiEipChargeMode.POSTPAID_BY_TRAFFIC)
                bandwidthOption.setChargeMode(CreatePublicipBandwidthOption.ChargeModeEnum.TRAFFIC);
            else
                bandwidthOption.setChargeMode(CreatePublicipBandwidthOption.ChargeModeEnum.BANDWIDTH);

            CreatePublicipOption option = new CreatePublicipOption();
            option.setIpVersion(
                    input.getIpVersion() == InternetProtocol.IPv6 ?
                            CreatePublicipOption.IpVersionEnum.NUMBER_6:
                            CreatePublicipOption.IpVersionEnum.NUMBER_4
            );
            option.setType(input.getEipType());
            option.setAlias(resource.getName());

            String eipId = provider.buildClient(account).eip().createEip(
                    new CreatePublicipRequest().withBody(
                            new CreatePublicipRequestBody().withBandwidth(bandwidthOption).withPublicip(
                                    option
                            )
                    )
            );
            resource.setExternalId(eipId);
        }
    }


    private static CreatePrePaidPublicipExtendParamOption getExtendParamOption(HuaweiEipBuildInput input) {
        var extendParamOption = new CreatePrePaidPublicipExtendParamOption();
        extendParamOption.setChargeMode(CreatePrePaidPublicipExtendParamOption.ChargeModeEnum.PREPAID);
        extendParamOption.setIsAutoRenew(input.isAutoRenew());

        int period = Integer.parseInt(input.getPeriod());
        int periodYears = period / 12;
        int periodMonths = period % 12;

        if(periodYears > 0){
            extendParamOption.setPeriodNum(periodYears);
            extendParamOption.setPeriodType(CreatePrePaidPublicipExtendParamOption.PeriodTypeEnum.YEAR);
        }else {
            extendParamOption.setPeriodNum(periodMonths);
            extendParamOption.setPeriodType(CreatePrePaidPublicipExtendParamOption.PeriodTypeEnum.MONTH);
        }
        return extendParamOption;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of(
                new ResourceUsage(
                        UsageTypes.ELASTIC_IP.type(),
                        BigDecimal.ONE
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }


    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, HuaweiEipBuildInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        String bandwidthInquiryId = UUID.randomUUID().toString();
        String ipInquiryId = UUID.randomUUID().toString();

        boolean isStatic = Objects.equals(input.getEipType(), "5_sbgp");
        boolean isShared = input.getShareType() == HuaweiEipShareType.WHOLE;

        if(input.getChargeMode() == HuaweiEipChargeMode.PREPAID_BY_BANDWIDTH){
            CreatePrePaidPublicipExtendParamOption extendParamOption = getExtendParamOption(input);

            int periodTypeNumber;
            ChronoUnit timeUnit;
            if(CreatePrePaidPublicipExtendParamOption.PeriodTypeEnum.YEAR.equals(extendParamOption.getPeriodType())) {
                periodTypeNumber = 3;
                timeUnit = ChronoUnit.YEARS;
            } else {
                periodTypeNumber = 2;
                timeUnit = ChronoUnit.MONTHS;
            }

            String resourceSpec;
            if(isShared){
                resourceSpec = "19_share";
            } else {
                if(isStatic)
                    resourceSpec = "19_sbgp";
                else
                    resourceSpec = "19_bgp";
            }

            PeriodProductInfo bandwidthInfo = new PeriodProductInfo()
                    .withId(bandwidthInquiryId)
                    .withCloudServiceType("hws.service.type.vpc")
                    .withResourceType("hws.resource.type.bandwidth")
                    .withResourceSpec(resourceSpec)
                    .withRegion(client.getRegionId())
                    .withPeriodType(periodTypeNumber)
                    .withPeriodNum(extendParamOption.getPeriodNum())
                    .withResourceSize(input.getBandwidthSize())
                    .withSizeMeasureId(15)
                    .withSubscriptionNum(1);

            PeriodProductInfo ipInfo = new PeriodProductInfo()
                    .withId(ipInquiryId)
                    .withCloudServiceType("hws.service.type.vpc")
                    .withResourceType("hws.resource.type.ip")
                    .withResourceSpec(isStatic ? "5_sbgp" : "5_bgp")
                    .withRegion(client.getRegionId())
                    .withPeriodType(periodTypeNumber)
                    .withPeriodNum(extendParamOption.getPeriodNum())
                    .withSubscriptionNum(1);

            ListRateOnPeriodDetailRequest request = new ListRateOnPeriodDetailRequest();
            request.withBody(
                    new RateOnPeriodReq().withProductInfos(
                            List.of(bandwidthInfo, ipInfo)
                    ).withProjectId(client.getProjectId())
            );

            ListRateOnPeriodDetailResponse response = client.bss().inquiryPeriodResources(request);
            BigDecimal cost = response.getOfficialWebsiteRatingResult().getOfficialWebsiteAmount();
            return new ResourceCost(
                    cost.doubleValue(),
                    1.0,
                    timeUnit
            );
        } else if(input.getChargeMode() == HuaweiEipChargeMode.POSTPAID_BY_BANDWIDTH) {
            String resourceSpec;

            if (isShared) {
                resourceSpec = "19_share";
            } else {
                if (isStatic)
                    resourceSpec = "19_sbgp";
                else
                    resourceSpec = "19_bgp";
            }


            DemandProductInfo bandwidthInfo = new DemandProductInfo()
                    .withId(bandwidthInquiryId)
                    .withCloudServiceType("hws.service.type.vpc")
                    .withResourceType("hws.resource.type.bandwidth")
                    .withResourceSpec(resourceSpec)
                    .withRegion(client.getRegionId())
                    .withUsageFactor("Duration")
                    .withUsageMeasureId(4)
                    .withUsageValue(BigDecimal.ONE)
                    .withResourceSize(input.getBandwidthSize())
                    .withSizeMeasureId(15)
                    .withSubscriptionNum(1);

            DemandProductInfo ipInfo = new DemandProductInfo()
                    .withId(ipInquiryId)
                    .withCloudServiceType("hws.service.type.vpc")
                    .withResourceType("hws.resource.type.ip")
                    .withResourceSpec(isStatic ? "5_sbgp" : "5_bgp")
                    .withRegion(client.getRegionId())
                    .withUsageFactor("Duration")
                    .withUsageMeasureId(4)
                    .withUsageValue(BigDecimal.ONE)
                    .withSubscriptionNum(1);


            var request = new ListOnDemandResourceRatingsRequest();
            request.withBody(
                    new RateOnDemandReq().withProductInfos(
                            List.of(bandwidthInfo, ipInfo)
                    ).withProjectId(client.getProjectId())
            );

            var response = client.bss().inquiryOnDemandResources(request);

            BigDecimal cost = response.getAmount();
            return new ResourceCost(
                    cost.doubleValue(),
                    1.0,
                    ChronoUnit.HOURS
            );
        } else {
            return ResourceCost.ZERO;
        }
    }
}
