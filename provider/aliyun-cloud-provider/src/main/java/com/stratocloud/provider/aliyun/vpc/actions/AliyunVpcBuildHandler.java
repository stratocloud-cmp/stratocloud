package com.stratocloud.provider.aliyun.vpc.actions;

import com.aliyun.vpc20160428.models.CreateVpcRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
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
public class AliyunVpcBuildHandler implements BuildResourceActionHandler {

    private final AliyunVpcHandler vpcHandler;


    public AliyunVpcBuildHandler(AliyunVpcHandler vpcHandler) {
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
        return AliyunVpcBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        CreateVpcRequest request = buildRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) vpcHandler.getProvider();

        String vpcId = provider.buildClient(account).vpc().createVpc(request);
        resource.setExternalId(vpcId);
    }

    private static CreateVpcRequest buildRequest(Resource resource, Map<String, Object> parameters) {
        AliyunVpcBuildInput input = JSON.convert(parameters, AliyunVpcBuildInput.class);

        CreateVpcRequest request = new CreateVpcRequest();

        request.setVpcName(resource.getName());
        request.setCidrBlock(input.getCidrBlock());

        request.setDescription(resource.getDescription());

        if(input.getEnableIpv6() != null && input.getEnableIpv6()){
            request.setEnableIpv6(input.getEnableIpv6());
            request.setIpv6CidrBlock(input.getIpv6CidrBlock());
            request.setIpv6Isp(input.getIpv6Isp());
        }
        return request;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        CreateVpcRequest request = buildRequest(resource, parameters);
        request.setDryRun(true);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) vpcHandler.getProvider();

        provider.buildClient(account).vpc().createVpc(request);
    }
}
