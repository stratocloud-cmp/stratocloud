package com.stratocloud.provider.huawei.securitygroup.actions;

import com.huaweicloud.sdk.vpc.v2.model.SecurityGroup;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSecurityGroupDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiSecurityGroupDestroyHandler(HuaweiSecurityGroupHandler securityGroupHandler) {
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return securityGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "删除安全组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<SecurityGroup> securityGroup = securityGroupHandler.describeSecurityGroup(
                account, resource.getExternalId()
        );

        if(securityGroup.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) securityGroupHandler.getProvider();

        provider.buildClient(account).vpc().deleteSecurityGroup(securityGroup.get().getId());
    }
}
