package com.stratocloud.provider.aliyun.subnet.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.aliyun.subnet.requirements.AliyunSubnetToVpcHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunSubnetDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunSubnetHandler subnetHandler;


    public AliyunSubnetDestroyHandler(AliyunSubnetHandler subnetHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) subnetHandler.getProvider();

        provider.buildClient(account).vpc().deleteSubnet(resource.getExternalId());
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(AliyunSubnetToVpcHandler.TYPE_ID);
    }
}
