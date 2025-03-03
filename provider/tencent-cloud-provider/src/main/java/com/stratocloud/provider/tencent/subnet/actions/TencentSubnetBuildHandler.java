package com.stratocloud.provider.tencent.subnet.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.vpc.v20170312.models.CreateSubnetRequest;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentSubnetBuildHandler implements BuildResourceActionHandler {

    private final TencentSubnetHandler subnetHandler;

    public TencentSubnetBuildHandler(TencentSubnetHandler subnetHandler) {
        this.subnetHandler = subnetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return subnetHandler;
    }

    @Override
    public String getTaskName() {
        return "创建子网";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentSubnetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentSubnetBuildInput input = JSON.convert(parameters, TencentSubnetBuildInput.class);

        Resource vpc = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating subnet.")
        );
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not found when creating subnet.")
        );

        CreateSubnetRequest request = new CreateSubnetRequest();
        request.setVpcId(vpc.getExternalId());
        request.setZone(zone.getExternalId());

        request.setSubnetName(resource.getName());
        request.setCidrBlock(input.getCidrBlock());


        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) subnetHandler.getProvider();

        Subnet subnet = provider.buildClient(account).createSubnet(request);
        resource.setExternalId(subnet.getSubnetId());
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
