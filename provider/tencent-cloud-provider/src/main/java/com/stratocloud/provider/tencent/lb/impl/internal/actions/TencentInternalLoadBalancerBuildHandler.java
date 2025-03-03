package com.stratocloud.provider.tencent.lb.impl.internal.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.impl.internal.TencentInternalLoadBalancerHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TencentInternalLoadBalancerBuildHandler implements BuildResourceActionHandler {

    private final TencentInternalLoadBalancerHandler loadBalancerHandler;

    public TencentInternalLoadBalancerBuildHandler(TencentInternalLoadBalancerHandler loadBalancerHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
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
        return TencentInternalLoadBalancerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var input = JSON.convert(parameters, TencentInternalLoadBalancerBuildInput.class);

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating open lb.")
        );

        Resource vpc = subnet.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating open lb.")
        );

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest();
        request.setLoadBalancerType("INTERNAL");
        request.setLoadBalancerName(resource.getName());
        request.setVpcId(vpc.getExternalId());
        request.setSubnetId(subnet.getExternalId());

        if(Utils.isNotBlank(input.getSlaType())) {
            request.setSlaType(input.getSlaType());

            InternetAccessible internetAccessible = new InternetAccessible();
            internetAccessible.setInternetChargeType(input.getInternetChargeType());
            internetAccessible.setInternetMaxBandwidthOut(Long.valueOf(input.getInternetMaxBandwidthOut()));
            request.setInternetAccessible(internetAccessible);
        }

        request.setDynamicVip(input.getDynamicVip());


        TencentCloudProvider provider = (TencentCloudProvider) loadBalancerHandler.getProvider();
        String lbId = provider.buildClient(account).createLoadBalancer(request);
        resource.setExternalId(lbId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, TencentInternalLoadBalancerBuildInput.class);

        if(Utils.isEmpty(input.getVips()))
            return List.of();

        return List.of(
                new ResourceUsage(
                        UsageTypes.NIC_IP.type(),
                        BigDecimal.ONE
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }


    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, TencentInternalLoadBalancerBuildInput.class);
        var request = new InquiryPriceCreateLoadBalancerRequest();

        request.setLoadBalancerType("INTERNAL");
        request.setLoadBalancerChargeType("POSTPAID");

        if(Utils.isNotBlank(input.getSlaType())) {
            request.setSlaType(input.getSlaType());

            InternetAccessible internetAccessible = new InternetAccessible();
            internetAccessible.setInternetChargeType(input.getInternetChargeType());
            internetAccessible.setInternetMaxBandwidthOut(Long.valueOf(input.getInternetMaxBandwidthOut()));
            request.setInternetAccessible(internetAccessible);
        }

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) loadBalancerHandler.getProvider();
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
