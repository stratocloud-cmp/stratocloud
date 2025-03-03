package com.stratocloud.provider.huawei.elb.health.actions;

import com.huaweicloud.sdk.elb.v3.model.CreateHealthMonitorOption;
import com.huaweicloud.sdk.elb.v3.model.CreateHealthMonitorRequest;
import com.huaweicloud.sdk.elb.v3.model.CreateHealthMonitorRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.health.HuaweiHealthMonitorHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiHealthMonitorBuildHandler implements BuildResourceActionHandler {

    private final HuaweiHealthMonitorHandler healthMonitorHandler;

    public HuaweiHealthMonitorBuildHandler(HuaweiHealthMonitorHandler healthMonitorHandler) {
        this.healthMonitorHandler = healthMonitorHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return healthMonitorHandler;
    }

    @Override
    public String getTaskName() {
        return "创建健康检查";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiHealthMonitorBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, HuaweiHealthMonitorBuildInput.class);
        Resource pool = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_BACKEND_GROUP).orElseThrow(
                () -> new StratoException("Pool not found when creating health monitor.")
        );

        CreateHealthMonitorOption healthMonitorOption = new CreateHealthMonitorOption();

        healthMonitorOption
                .withName(resource.getName())
                .withType(input.getType())
                .withDelay(input.getDelay())
                .withMaxRetries(input.getMaxRetries())
                .withMaxRetriesDown(input.getMaxRetriesDown())
                .withTimeout(input.getTimeout())
                .withPoolId(pool.getExternalId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) healthMonitorHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String monitorId = provider.buildClient(account).elb().createHealthMonitor(
                new CreateHealthMonitorRequest().withBody(
                        new CreateHealthMonitorRequestBody().withHealthmonitor(healthMonitorOption)
                )
        );
        resource.setExternalId(monitorId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
