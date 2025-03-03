package com.stratocloud.provider.tencent.securitygroup.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.CreateSecurityGroupRequest;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentSecurityGroupBuildHandler implements BuildResourceActionHandler {

    private final TencentSecurityGroupHandler securityGroupHandler;


    public TencentSecurityGroupBuildHandler(TencentSecurityGroupHandler securityGroupHandler) {
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
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) securityGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setGroupName(resource.getName());

        if(Utils.isBlank(resource.getDescription()))
            request.setGroupDescription(resource.getName());
        else
            request.setGroupDescription(resource.getDescription());

        SecurityGroup securityGroup = provider.buildClient(account).createSecurityGroup(request);

        resource.setExternalId(securityGroup.getSecurityGroupId());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
