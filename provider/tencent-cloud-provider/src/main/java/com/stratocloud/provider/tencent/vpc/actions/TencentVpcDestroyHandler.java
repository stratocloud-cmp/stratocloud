package com.stratocloud.provider.tencent.vpc.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.vpc.TencentVpcHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.Vpc;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentVpcDestroyHandler implements DestroyResourceActionHandler {

    private final TencentVpcHandler vpcHandler;

    public TencentVpcDestroyHandler(TencentVpcHandler vpcHandler) {
        this.vpcHandler = vpcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return vpcHandler;
    }


    @Override
    public String getTaskName() {
        return "删除私有网络";
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
        TencentCloudProvider provider = (TencentCloudProvider) vpcHandler.getProvider();

        Optional<Vpc> vpc = vpcHandler.describeVpc(account, resource.getExternalId());

        if(vpc.isEmpty())
            return;

        provider.buildClient(account).deleteVpc(resource.getExternalId());
    }
}
