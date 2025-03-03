package com.stratocloud.provider.huawei.subnet.actions;

import com.huaweicloud.sdk.vpc.v2.model.CreateSubnetOption;
import com.huaweicloud.sdk.vpc.v2.model.CreateSubnetRequest;
import com.huaweicloud.sdk.vpc.v2.model.CreateSubnetRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
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
public class HuaweiSubnetBuildHandler implements BuildResourceActionHandler {

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiSubnetBuildHandler(HuaweiSubnetHandler subnetHandler) {
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
        return HuaweiSubnetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiSubnetBuildInput input = JSON.convert(parameters, HuaweiSubnetBuildInput.class);

        Resource vpc = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("VPC not provided when creating subnet.")
        );

        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not provided when creating subnet.")
        );

        CreateSubnetOption option = new CreateSubnetOption();
        option.setName(resource.getName());
        option.setDescription(resource.getDescription());
        option.setCidr(input.getCidr());

        option.setGatewayIp(input.getGatewayIp());
        option.setIpv6Enable(input.isEnableIpv6());

        option.setVpcId(vpc.getExternalId());
        option.setAvailabilityZone(zone.getExternalId());

        CreateSubnetRequest request = new CreateSubnetRequest().withBody(
                new CreateSubnetRequestBody().withSubnet(option)
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) subnetHandler.getProvider();

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
}
