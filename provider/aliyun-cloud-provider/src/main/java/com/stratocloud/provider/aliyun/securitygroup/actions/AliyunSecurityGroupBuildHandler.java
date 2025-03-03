package com.stratocloud.provider.aliyun.securitygroup.actions;

import com.aliyun.ecs20140526.models.CreateSecurityGroupRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
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
public class AliyunSecurityGroupBuildHandler implements BuildResourceActionHandler {

    private final AliyunSecurityGroupHandler securityGroupHandler;


    public AliyunSecurityGroupBuildHandler(AliyunSecurityGroupHandler securityGroupHandler) {
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return securityGroupHandler;
    }


    @Override
    public String getTaskName() {
        return "创建安全组";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunSecurityGroupBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) securityGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunSecurityGroupBuildInput input = JSON.convert(parameters, AliyunSecurityGroupBuildInput.class);

        Resource vpc = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating security group.")
        );

        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setVpcId(vpc.getExternalId());
        request.setSecurityGroupName(resource.getName());
        request.setSecurityGroupType(input.getSecurityGroupType());

        request.setDescription(resource.getDescription());


        String securityGroupId = provider.buildClient(account).ecs().createSecurityGroup(request);

        resource.setExternalId(securityGroupId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
