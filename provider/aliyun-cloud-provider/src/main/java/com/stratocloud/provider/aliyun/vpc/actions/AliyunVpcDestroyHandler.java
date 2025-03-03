package com.stratocloud.provider.aliyun.vpc.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.vpc.AliyunVpc;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunVpcDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunVpcHandler vpcHandler;

    public AliyunVpcDestroyHandler(AliyunVpcHandler vpcHandler) {
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
        performDelete(resource, false);
    }

    private void performDelete(Resource resource, boolean dryRun) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) vpcHandler.getProvider();

        Optional<AliyunVpc> vpc = vpcHandler.describeVpc(account, resource.getExternalId());

        if(vpc.isEmpty())
            return;

        provider.buildClient(account).vpc().deleteVpc(resource.getExternalId(), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        performDelete(resource, true);
    }
}
