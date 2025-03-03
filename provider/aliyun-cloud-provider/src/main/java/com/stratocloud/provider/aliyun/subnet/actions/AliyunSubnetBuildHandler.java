package com.stratocloud.provider.aliyun.subnet.actions;

import com.aliyun.vpc20160428.models.CreateVSwitchRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.aliyun.subnet.requirements.AliyunSubnetToVpcHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunSubnetBuildHandler implements BuildResourceActionHandler {

    private final AliyunSubnetHandler subnetHandler;

    public AliyunSubnetBuildHandler(AliyunSubnetHandler subnetHandler) {
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
        return AliyunSubnetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunSubnetBuildInput input = JSON.convert(parameters, AliyunSubnetBuildInput.class);

        Resource vpc = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating subnet.")
        );
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not found when creating subnet.")
        );

        CreateVSwitchRequest request = new CreateVSwitchRequest();
        request.setVpcId(vpc.getExternalId());
        request.setZoneId(zone.getExternalId());

        request.setVSwitchName(resource.getName());
        request.setCidrBlock(input.getCidrBlock());

        request.setDescription(resource.getDescription());

        if(input.getEnableIpv6() != null && input.getEnableIpv6()){
            request.setIpv6CidrBlock(input.getIpv6CidrBlock());
        }



        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) subnetHandler.getProvider();

        String subnetId = provider.buildClient(account).vpc().createSubnet(request);
        resource.setExternalId(subnetId);
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }


    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(AliyunSubnetToVpcHandler.TYPE_ID);
    }
}
