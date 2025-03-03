package com.stratocloud.provider.huawei.securitygroup.actions;

import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupOption;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRequest;
import com.huaweicloud.sdk.vpc.v2.model.CreateSecurityGroupRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiSecurityGroupBuildHandler implements BuildResourceActionHandler {

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiSecurityGroupBuildHandler(HuaweiSecurityGroupHandler securityGroupHandler) {
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
        CreateSecurityGroupOption option = new CreateSecurityGroupOption();
        option.setName(resource.getName());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) securityGroupHandler.getProvider();

        String securityGroupId = provider.buildClient(account).vpc().createSecurityGroup(
                new CreateSecurityGroupRequest().withBody(
                        new CreateSecurityGroupRequestBody().withSecurityGroup(option)
                )
        );
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
