package com.stratocloud.provider.huawei.subnet.actions;

import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSubnetDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiSubnetDestroyHandler(HuaweiSubnetHandler subnetHandler) {
        this.subnetHandler = subnetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return subnetHandler;
    }

    @Override
    public String getTaskName() {
        return "删除子网";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiSubnetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<Subnet> subnet = subnetHandler.describeSubnet(account, resource.getExternalId());

        if(subnet.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) subnetHandler.getProvider();

        provider.buildClient(account).vpc().deleteSubnet(subnet.get());
    }
}
