package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.aliyun.slb20140515.models.CreateLoadBalancerRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AliyunIntranetClbBuildHandler implements BuildResourceActionHandler {

    private final AliyunIntranetClbHandler clbHandler;

    public AliyunIntranetClbBuildHandler(AliyunIntranetClbHandler clbHandler) {
        this.clbHandler = clbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return clbHandler;
    }

    @Override
    public String getTaskName() {
        return "创建内网CLB实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunIntranetClbBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunIntranetClbBuildInput input = JSON.convert(parameters, AliyunIntranetClbBuildInput.class);

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest();
        request.setAddressIPVersion(input.getIpVersion());
        request.setAddressType("intranet");
        request.setPayType("PayOnDemand");
        request.setDeleteProtection("off");

        request.setInstanceChargeType(input.getInstanceChargeType());
        if(Objects.equals("PayBySpec", input.getInstanceChargeType())){
            request.setInternetChargeType(input.getInternetChargeType());

            request.setLoadBalancerSpec(input.getSpec());

            if(Objects.equals("paybybandwidth", input.getInternetChargeType())){
                request.setBandwidth(input.getBandwidth());
            }
        }else if(Objects.equals("PayByCLCU", input.getInstanceChargeType())){
            request.setInternetChargeType("paybytraffic");
        }else{
            throw new StratoException(
                    "Unknown instanceChargeType %s for clb.".formatted(input.getInstanceChargeType())
            );
        }

        Resource masterZone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Master zone not found when creating clb.")
        );

        request.setMasterZoneId(masterZone.getExternalId());

        if(Utils.isNotBlank(input.getSlaveZoneId()))
            request.setSlaveZoneId(input.getSlaveZoneId());

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating clb.")
        );

        Resource vpc = subnet.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("VPC not found when creating clb.")
        );

        request.setVpcId(vpc.getExternalId());
        request.setVSwitchId(subnet.getExternalId());

        if(Utils.isNotEmpty(input.getIpAddress()))
            request.setAddress(input.getIpAddress().get(0));


        request.setLoadBalancerName(resource.getName());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) clbHandler.getProvider();

        String lbId = provider.buildClient(account).clb().createLoadBalancer(request);
        resource.setExternalId(lbId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
