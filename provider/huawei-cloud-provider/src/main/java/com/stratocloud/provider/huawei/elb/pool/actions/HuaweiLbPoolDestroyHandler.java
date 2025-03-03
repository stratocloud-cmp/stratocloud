package com.stratocloud.provider.huawei.elb.pool.actions;

import com.huaweicloud.sdk.elb.v3.model.Pool;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiLbPoolDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiLbPoolHandler lbPoolHandler;

    public HuaweiLbPoolDestroyHandler(HuaweiLbPoolHandler lbPoolHandler) {
        this.lbPoolHandler = lbPoolHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return lbPoolHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁后端服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<Pool> pool = lbPoolHandler.describeLbPool(account, resource.getExternalId());

        if(pool.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) lbPoolHandler.getProvider();

        provider.buildClient(account).elb().deleteLbPool(pool.get().getId());
    }
}
