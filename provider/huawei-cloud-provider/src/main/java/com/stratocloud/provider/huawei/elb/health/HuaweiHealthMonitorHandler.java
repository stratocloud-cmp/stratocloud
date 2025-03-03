package com.stratocloud.provider.huawei.elb.health;

import com.huaweicloud.sdk.elb.v3.model.HealthMonitor;
import com.huaweicloud.sdk.elb.v3.model.ListHealthMonitorsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLbStatusTreeHelper;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiHealthMonitorHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiHealthMonitorHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_HEALTH_MONITOR";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云健康检查";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_HEALTH_MONITOR;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {

        return describeMonitor(account, externalId).map(
                m -> toExternalResource(account, m)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, HealthMonitor healthMonitor) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                healthMonitor.getId(),
                healthMonitor.getName(),
                HuaweiLbStatusTreeHelper.getHealthMonitorState(provider, account.getId(), healthMonitor)
        );
    }

    public Optional<HealthMonitor> describeMonitor(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describeMonitor(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<HealthMonitor> monitors = provider.buildClient(account).elb().describeMonitors(
                new ListHealthMonitorsRequest()
        );
        return monitors.stream().map(m -> toExternalResource(account, m)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var monitorV2 = describeMonitor(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Health monitor not found.")
        );

        resource.updateByExternal(toExternalResource(account, monitorV2));

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type", "类型", monitorV2.getType(), monitorV2.getType()
        );
        resource.addOrUpdateRuntimeProperty(typeProperty);

        if(monitorV2.getDelay() != null){
            RuntimeProperty delayProperty = RuntimeProperty.ofDisplayInList(
                    "delay",
                    "健康检查间隔(秒)",
                    monitorV2.getDelay().toString(),
                    monitorV2.getDelay().toString()
            );
            resource.addOrUpdateRuntimeProperty(delayProperty);
        }

        if(monitorV2.getTimeout() != null){
            RuntimeProperty timeoutProperty = RuntimeProperty.ofDisplayInList(
                    "timeout",
                    "健康检查超时时间(秒)",
                    monitorV2.getTimeout().toString(),
                    monitorV2.getTimeout().toString()
            );
            resource.addOrUpdateRuntimeProperty(timeoutProperty);
        }

        if(monitorV2.getMaxRetries() != null){
            RuntimeProperty maxRetriesProperty = RuntimeProperty.ofDisplayable(
                    "maxRetries",
                    "置为健康所需次数",
                    monitorV2.getMaxRetries().toString(),
                    monitorV2.getMaxRetries().toString()
            );
            resource.addOrUpdateRuntimeProperty(maxRetriesProperty);
        }

        if(monitorV2.getMaxRetriesDown() != null){
            RuntimeProperty maxRetriesDownProperty = RuntimeProperty.ofDisplayable(
                    "maxRetriesDown",
                    "置为不健康所需次数",
                    monitorV2.getMaxRetriesDown().toString(),
                    monitorV2.getMaxRetriesDown().toString()
            );
            resource.addOrUpdateRuntimeProperty(maxRetriesDownProperty);
        }

        if(Utils.isNotBlank(monitorV2.getHttpMethod())){
            RuntimeProperty httpMethodProperty = RuntimeProperty.ofDisplayable(
                    "httpMethod", "HTTP方法", monitorV2.getHttpMethod(), monitorV2.getHttpMethod()
            );
            resource.addOrUpdateRuntimeProperty(httpMethodProperty);
        }

        if(Utils.isNotBlank(monitorV2.getUrlPath())){
            RuntimeProperty urlPathProperty = RuntimeProperty.ofDisplayable(
                    "urlPath", "检查路径", monitorV2.getUrlPath(), monitorV2.getUrlPath()
            );
            resource.addOrUpdateRuntimeProperty(urlPathProperty);
        }

        if(Utils.isNotBlank(monitorV2.getExpectedCodes())){
            RuntimeProperty expectedCodesProperty = RuntimeProperty.ofDisplayable(
                    "expectedCodes",
                    "期望响应码",
                    monitorV2.getExpectedCodes(),
                    monitorV2.getExpectedCodes()
            );
            resource.addOrUpdateRuntimeProperty(expectedCodesProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
