package com.stratocloud.provider.tencent.vpc.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.vpc.TencentVpcHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.CreateVpcRequest;
import com.tencentcloudapi.vpc.v20170312.models.Vpc;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentVpcBuildHandler implements BuildResourceActionHandler {

    private final TencentVpcHandler vpcHandler;


    public TencentVpcBuildHandler(TencentVpcHandler vpcHandler) {
        this.vpcHandler = vpcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return vpcHandler;
    }

    @Override
    public String getTaskName() {
        return "创建私有网络";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentVpcBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentVpcBuildInput input = JSON.convert(parameters, TencentVpcBuildInput.class);

        CreateVpcRequest request = new CreateVpcRequest();

        request.setVpcName(resource.getName());
        request.setCidrBlock(input.getCidrBlock());
        request.setEnableMulticast(input.getEnableMultiCast().toString());

        if(Utils.isNotEmpty(input.getDnsServers()))
            request.setDnsServers(input.getDnsServers().toArray(new String[0]));

        request.setDomainName(input.getDomainName());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) vpcHandler.getProvider();

        Vpc vpc = provider.buildClient(account).createVpc(request);
        resource.setExternalId(vpc.getVpcId());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
