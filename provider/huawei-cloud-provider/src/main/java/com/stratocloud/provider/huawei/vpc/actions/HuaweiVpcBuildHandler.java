package com.stratocloud.provider.huawei.vpc.actions;

import com.huaweicloud.sdk.vpc.v2.model.CreateVpcOption;
import com.huaweicloud.sdk.vpc.v2.model.CreateVpcRequest;
import com.huaweicloud.sdk.vpc.v2.model.CreateVpcRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.vpc.HuaweiVpcHandler;
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
public class HuaweiVpcBuildHandler implements BuildResourceActionHandler {

    private final HuaweiVpcHandler vpcHandler;

    public HuaweiVpcBuildHandler(HuaweiVpcHandler vpcHandler) {
        this.vpcHandler = vpcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return vpcHandler;
    }

    @Override
    public String getTaskName() {
        return "创建VPC";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiVpcBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiVpcBuildInput input = JSON.convert(parameters, HuaweiVpcBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) vpcHandler.getProvider();

        CreateVpcRequest request = new CreateVpcRequest();
        request.setBody(
                new CreateVpcRequestBody().withVpc(
                        new CreateVpcOption()
                                .withName(resource.getName())
                                .withDescription(resource.getDescription())
                                .withCidr(input.getCidr())
                )
        );

        String vpcId = provider.buildClient(account).vpc().createVpc(request);
        resource.setExternalId(vpcId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
