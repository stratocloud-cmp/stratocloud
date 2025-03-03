package com.stratocloud.provider.huawei.elb.health.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.health.HuaweiHealthMonitorHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HuaweiHealthMonitorDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiHealthMonitorHandler healthMonitorHandler;

    public HuaweiHealthMonitorDestroyHandler(HuaweiHealthMonitorHandler healthMonitorHandler) {
        this.healthMonitorHandler = healthMonitorHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return healthMonitorHandler;
    }

    @Override
    public String getTaskName() {
        return "移除健康检查";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) healthMonitorHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var monitor = healthMonitorHandler.describeMonitor(account, resource.getExternalId());

        if(monitor.isEmpty())
            return;

        provider.buildClient(account).elb().deleteHealthMonitor(monitor.get().getId());
    }
}
