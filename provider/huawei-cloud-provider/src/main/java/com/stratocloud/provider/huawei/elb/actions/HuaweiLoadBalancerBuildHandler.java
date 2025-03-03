package com.stratocloud.provider.huawei.elb.actions;

import com.huaweicloud.sdk.bss.v2.model.*;
import com.huaweicloud.sdk.elb.v3.model.*;
import com.huaweicloud.sdk.vpc.v2.model.ListSubnetsRequest;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.ProviderStockException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.elb.requirements.HuaweiElbToBackendSubnetHandler;
import com.stratocloud.provider.huawei.elb.requirements.HuaweiElbToL4FlavorHandler;
import com.stratocloud.provider.huawei.elb.requirements.HuaweiElbToL7FlavorHandler;
import com.stratocloud.provider.huawei.elb.requirements.HuaweiElbToZoneHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
public class HuaweiLoadBalancerBuildHandler implements BuildResourceActionHandler {

    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final IpAllocator ipAllocator;


    public HuaweiLoadBalancerBuildHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
                                          IpAllocator ipAllocator) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.ipAllocator = ipAllocator;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return loadBalancerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建负载均衡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiLoadBalancerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        CreateLoadBalancerOption option = validateAndGetCreateOption(resource, parameters, client, true);

        String lbId = client.elb().createLoadBalancer(
                new CreateLoadBalancerRequest().withBody(
                        new CreateLoadBalancerRequestBody().withLoadbalancer(
                                option
                        )
                )
        );
        resource.setExternalId(lbId);
    }


    private CreateLoadBalancerOption validateAndGetCreateOption(Resource resource,
                                                                Map<String, Object> parameters,
                                                                HuaweiCloudClient client,
                                                                boolean allocateIps) {
        HuaweiLoadBalancerBuildInput input = JSON.convert(parameters, HuaweiLoadBalancerBuildInput.class);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided when creating LB.")
        );
        Optional<Resource> l4FlavorResource = resource.getExclusiveTargetByType(
                HuaweiElbToL4FlavorHandler.TYPE_ID
        );
        Optional<Resource> l7FlavorResource = resource.getExclusiveTargetByType(
                HuaweiElbToL7FlavorHandler.TYPE_ID
        );


        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found when creating LB.")
        );


        CreateLoadBalancerOption option = new CreateLoadBalancerOption();
        option.setName(resource.getName());
        option.setDescription(resource.getDescription());
        option.setVipSubnetCidrId(subnet.getNeutronSubnetId());

        if(subnet.getIpv6Enable() != null && subnet.getIpv6Enable() && input.isUsingIpv6())
            option.setIpv6VipVirsubnetId(subnet.getNeutronNetworkId());

        if(Utils.isNotEmpty(input.getIps())){
            String vipAddress = input.getIps().get(0);
            option.setVipAddress(vipAddress);

            if(allocateIps)
                ipAllocator.allocateIps(subnetResource, InternetProtocol.IPv4, List.of(vipAddress), resource);
        }

        if(l4FlavorResource.isPresent()){
            Flavor flavor = client.elb().describeFlavor(l4FlavorResource.get().getExternalId()).orElseThrow(
                    () -> new StratoException("Flavor not found when creating ELB.")
            );

            Boolean soldOut = flavor.getFlavorSoldOut();
            if(soldOut!=null && soldOut)
                throw new ProviderStockException("Flavor sold out.");

            option.setL4FlavorId(flavor.getId());
        }

        if(l7FlavorResource.isPresent()){
            Flavor flavor = client.elb().describeFlavor(l7FlavorResource.get().getExternalId()).orElseThrow(
                    () -> new StratoException("Flavor not found when creating ELB.")
            );

            Boolean soldOut = flavor.getFlavorSoldOut();
            if(soldOut!=null && soldOut)
                throw new ProviderStockException("Flavor sold out.");

            option.setL7FlavorId(flavor.getId());
        }

        List<Resource> zoneResources = resource.getRequirementTargets(
                resource.getRequirements(), HuaweiElbToZoneHandler.TYPE_ID
        );

        if(Utils.isNotEmpty(zoneResources)){
            option.setAvailabilityZoneList(
                    zoneResources.stream().map(Resource::getExternalId).toList()
            );
        } else {
            throw new BadCommandException("请选择至少一个可用区");
        }

        var backendSubnetIds = resource.getRequirements().stream().filter(
                rel -> rel.getType().equals(HuaweiElbToBackendSubnetHandler.TYPE_ID)
        ).map(Relationship::getTarget).map(Resource::getExternalId).filter(Utils::isNotBlank).toList();

        List<String> backendNeutronNetworkIds = client.vpc().describeSubnets(
                new ListSubnetsRequest()
        ).stream().filter(
                s -> backendSubnetIds.contains(s.getId())
        ).map(Subnet::getNeutronNetworkId).toList();

        if(Utils.isNotEmpty(backendNeutronNetworkIds))
            option.setElbVirsubnetIds(backendNeutronNetworkIds);

        option.setIpTargetEnable(input.isIpTargetEnabled());

        if(input.isPrepaid()){
            PrepaidCreateOption prepaidCreateOption = new PrepaidCreateOption();

            prepaidCreateOption.withAutoPay(true).withAutoRenew(input.isAutoRenew());

            int period = Integer.parseInt(input.getPeriod());
            int periodYears = period / 12;
            int periodMonths = period % 12;

            if(periodYears > 0)
                prepaidCreateOption.withPeriodType(PrepaidCreateOption.PeriodTypeEnum.YEAR)
                        .withPeriodNum(periodYears);
            else
                prepaidCreateOption.withPeriodType(PrepaidCreateOption.PeriodTypeEnum.MONTH)
                        .withPeriodNum(periodMonths);

            option.setPrepaidOptions(prepaidCreateOption);
        }
        return option;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        validateAndGetCreateOption(resource, parameters, client, false);
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        CreateLoadBalancerOption option = validateAndGetCreateOption(resource, parameters, client, false);
        PrepaidCreateOption prepaidOptions = option.getPrepaidOptions();

        if(Utils.isEmpty(option.getAvailabilityZoneList()))
            return ResourceCost.ZERO;

        String l4InquiryId = UUID.randomUUID().toString();
        String l7InquiryId = UUID.randomUUID().toString();

        if(prepaidOptions != null){
            PrepaidCreateOption.PeriodTypeEnum periodType = prepaidOptions.getPeriodType();
            int periodTypeNumber;
            ChronoUnit timeUnit;
            if(PrepaidCreateOption.PeriodTypeEnum.YEAR.equals(periodType)) {
                periodTypeNumber = 3;
                timeUnit = ChronoUnit.YEARS;
            } else {
                periodTypeNumber = 2;
                timeUnit = ChronoUnit.MONTHS;
            }

            List<PeriodProductInfo> productInfoList = new ArrayList<>();

            if(Utils.isNotBlank(option.getL4FlavorId())){
                Flavor flavor = client.elb().describeFlavor(option.getL4FlavorId()).orElseThrow(
                        () -> new StratoException("L4 flavor not found")
                );

                PeriodProductInfo productInfo = new PeriodProductInfo()
                        .withId(l4InquiryId)
                        .withCloudServiceType("hws.service.type.elb")
                        .withResourceType("hws.resource.type.elbv3")
                        .withResourceSpec("elbv3.basic.%saz".formatted(option.getAvailabilityZoneList().size()))
                        .withRegion(client.getRegionId())
                        .withPeriodType(periodTypeNumber)
                        .withPeriodNum(prepaidOptions.getPeriodNum())
                        .withResourceSize(flavor.getInfo().getLcu())
                        .withSizeMeasureId(14)
                        .withSubscriptionNum(1);

                productInfoList.add(productInfo);
            }

            if(Utils.isNotBlank(option.getL7FlavorId())){
                Flavor flavor = client.elb().describeFlavor(option.getL7FlavorId()).orElseThrow(
                        () -> new StratoException("L7 flavor not found")
                );

                PeriodProductInfo productInfo = new PeriodProductInfo()
                        .withId(l7InquiryId)
                        .withCloudServiceType("hws.service.type.elb")
                        .withResourceType("hws.resource.type.elbv3")
                        .withResourceSpec("elbv3.basic.%saz".formatted(option.getAvailabilityZoneList().size()))
                        .withRegion(client.getRegionId())
                        .withPeriodType(periodTypeNumber)
                        .withPeriodNum(prepaidOptions.getPeriodNum())
                        .withResourceSize(flavor.getInfo().getLcu())
                        .withSizeMeasureId(14)
                        .withSubscriptionNum(1);

                productInfoList.add(productInfo);
            }

            ListRateOnPeriodDetailRequest request = new ListRateOnPeriodDetailRequest();

            request.withBody(
                    new RateOnPeriodReq().withProductInfos(productInfoList).withProjectId(client.getProjectId())
            );

            ListRateOnPeriodDetailResponse response = client.bss().inquiryPeriodResources(request);
            BigDecimal cost = response.getOfficialWebsiteRatingResult().getOfficialWebsiteAmount();
            return new ResourceCost(
                    cost.doubleValue(),
                    1.0,
                    timeUnit
            );
        }else {
            List<DemandProductInfo> productInfoList = new ArrayList<>();

            if(Utils.isNotBlank(option.getL4FlavorId())){
                Flavor flavor = client.elb().describeFlavor(option.getL4FlavorId()).orElseThrow(
                        () -> new StratoException("L4 flavor not found")
                );

                DemandProductInfo productInfo = new DemandProductInfo()
                        .withId(l4InquiryId)
                        .withCloudServiceType("hws.service.type.elb")
                        .withResourceType("hws.resource.type.elbv3")
                        .withResourceSpec("elbv3.basic.%saz".formatted(option.getAvailabilityZoneList().size()))
                        .withRegion(client.getRegionId())
                        .withUsageFactor("l4_lcu_duration")
                        .withUsageMeasureId(4)
                        .withUsageValue(BigDecimal.ONE)
                        .withResourceSize(flavor.getInfo().getLcu())
                        .withSizeMeasureId(14)
                        .withSubscriptionNum(1);

                productInfoList.add(productInfo);
            }

            if(Utils.isNotBlank(option.getL7FlavorId())){
                Flavor flavor = client.elb().describeFlavor(option.getL7FlavorId()).orElseThrow(
                        () -> new StratoException("L7 flavor not found")
                );

                DemandProductInfo productInfo = new DemandProductInfo()
                        .withId(l7InquiryId)
                        .withCloudServiceType("hws.service.type.elb")
                        .withResourceType("hws.resource.type.elbv3")
                        .withResourceSpec("elbv3.basic.%saz".formatted(option.getAvailabilityZoneList().size()))
                        .withRegion(client.getRegionId())
                        .withUsageFactor("l7_lcu_duration")
                        .withUsageMeasureId(4)
                        .withUsageValue(BigDecimal.ONE)
                        .withResourceSize(flavor.getInfo().getLcu())
                        .withSizeMeasureId(14)
                        .withSubscriptionNum(1);

                productInfoList.add(productInfo);
            }

            var request = new ListOnDemandResourceRatingsRequest();

            request.withBody(
                    new RateOnDemandReq().withProductInfos(productInfoList).withProjectId(client.getProjectId())
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
}
