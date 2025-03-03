package com.stratocloud.provider.tencent.lb.impl.open.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.*;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TencentOpenLoadBalancerBuildHandler implements BuildResourceActionHandler {

    private final TencentOpenLoadBalancerHandler openLoadBalancerHandler;

    public TencentOpenLoadBalancerBuildHandler(TencentOpenLoadBalancerHandler openLoadBalancerHandler) {
        this.openLoadBalancerHandler = openLoadBalancerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return openLoadBalancerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建负载均衡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentOpenLoadBalancerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var input = JSON.convert(parameters, TencentOpenLoadBalancerBuildInput.class);

        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not found when creating open lb.")
        );

        Resource vpc = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating open lb.")
        );

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest();
        request.setLoadBalancerType("OPEN");
        request.setLoadBalancerName(resource.getName());
        request.setVpcId(vpc.getExternalId());
        request.setAddressIPVersion(input.getIpVersion());
        request.setZoneId(zone.getExternalId());

        InternetAccessible internetAccessible = new InternetAccessible();
        internetAccessible.setInternetChargeType(input.getInternetChargeType());
        internetAccessible.setInternetMaxBandwidthOut(Long.valueOf(input.getInternetMaxBandwidthOut()));
        request.setInternetAccessible(internetAccessible);

        request.setVipIsp(input.getIsp());

        if(Utils.isNotBlank(input.getSlaType()))
            request.setSlaType(input.getSlaType());

        request.setDynamicVip(input.getDynamicVip());


        TencentCloudProvider provider = (TencentCloudProvider) openLoadBalancerHandler.getProvider();
        String lbId = provider.buildClient(account).createLoadBalancer(request);
        resource.setExternalId(lbId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, TencentOpenLoadBalancerBuildInput.class);
        var request = new InquiryPriceCreateLoadBalancerRequest();

        request.setLoadBalancerType("OPEN");
        request.setLoadBalancerChargeType("POSTPAID");
        request.setAddressIPVersion(input.getIpVersion());
        request.setVipIsp(input.getIsp());


        if(Utils.isNotBlank(input.getSlaType())) {
            request.setSlaType(input.getSlaType());
        }

        InternetAccessible internetAccessible = new InternetAccessible();
        internetAccessible.setInternetChargeType(input.getInternetChargeType());
        internetAccessible.setInternetMaxBandwidthOut(Long.valueOf(input.getInternetMaxBandwidthOut()));
        request.setInternetAccessible(internetAccessible);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) openLoadBalancerHandler.getProvider();
        Price price = provider.buildClient(account).inquiryPriceCreateLoadBalancer(request);

        ResourceCost cost = getCost(price.getInstancePrice());
        cost = cost.add(getCost(price.getBandwidthPrice()));
        cost = cost.add(getCost(price.getLcuPrice()));

        return cost;
    }

    private ResourceCost getCost(ItemPrice itemPrice){
        if(itemPrice == null)
            return ResourceCost.ZERO;

        if(!Objects.equals("HOUR", itemPrice.getChargeUnit()))
            return ResourceCost.ZERO;

        return new ResourceCost(
                itemPrice.getUnitPriceDiscount(),
                1.0,
                ChronoUnit.HOURS
        );
    }
}
