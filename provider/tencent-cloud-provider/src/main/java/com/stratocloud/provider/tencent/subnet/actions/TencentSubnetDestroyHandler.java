package com.stratocloud.provider.tencent.subnet.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TencentSubnetDestroyHandler implements DestroyResourceActionHandler {

    private final TencentSubnetHandler subnetHandler;


    public TencentSubnetDestroyHandler(TencentSubnetHandler subnetHandler) {
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
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) subnetHandler.getProvider();

        provider.buildClient(account).deleteSubnet(resource.getExternalId());
    }
}
