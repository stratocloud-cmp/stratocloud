package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.AliyunClb;
import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunInternetClbDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunInternetClbHandler clbHandler;

    public AliyunInternetClbDestroyHandler(AliyunInternetClbHandler clbHandler) {
        this.clbHandler = clbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return clbHandler;
    }

    @Override
    public String getTaskName() {
        return "删除公网CLB";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunClb> clb = clbHandler.describeClb(account, resource.getExternalId());

        if(clb.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) clbHandler.getProvider();

        provider.buildClient(account).clb().deleteLoadBalancer(clb.get().detail().getLoadBalancerId());
    }
}
