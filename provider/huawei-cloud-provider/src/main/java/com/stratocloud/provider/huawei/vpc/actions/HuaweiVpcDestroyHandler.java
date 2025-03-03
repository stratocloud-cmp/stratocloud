package com.stratocloud.provider.huawei.vpc.actions;

import com.huaweicloud.sdk.vpc.v2.model.Vpc;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.vpc.HuaweiVpcHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiVpcDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiVpcHandler vpcHandler;

    public HuaweiVpcDestroyHandler(HuaweiVpcHandler vpcHandler) {
        this.vpcHandler = vpcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return vpcHandler;
    }

    @Override
    public String getTaskName() {
        return "删除VPC";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) vpcHandler.getProvider();

        Optional<Vpc> vpc = vpcHandler.describeVpc(account, resource.getExternalId());

        if(vpc.isEmpty())
            return;

        provider.buildClient(account).vpc().deleteVpc(vpc.get().getId());
    }
}
