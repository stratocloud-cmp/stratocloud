package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.aliyun.slb20140515.models.CreateLoadBalancerRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
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
public class AliyunInternetClbBuildHandler implements BuildResourceActionHandler {

    private final AliyunInternetClbHandler clbHandler;

    public AliyunInternetClbBuildHandler(AliyunInternetClbHandler clbHandler) {
        this.clbHandler = clbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return clbHandler;
    }

    @Override
    public String getTaskName() {
        return "创建公网CLB实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunClbBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunClbBuildInput input = JSON.convert(parameters, AliyunClbBuildInput.class);

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest();
        request.setAddressIPVersion(input.getIpVersion());
        request.setAddressType("internet");
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
