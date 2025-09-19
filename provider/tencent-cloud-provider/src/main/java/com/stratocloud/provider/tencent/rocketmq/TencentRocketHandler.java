package com.stratocloud.provider.tencent.rocketmq;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.trocket.v20230308.models.DescribeInstanceListRequest;
import com.tencentcloudapi.trocket.v20230308.models.DescribeInstanceResponse;
import com.tencentcloudapi.trocket.v20230308.models.InstanceItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentRocketHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentRocketHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_ROCKETMQ";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云RocketMQ实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.MQ_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<InstanceItem> rocket = describeRocket(account, externalId);

        return rocket.map(i -> toExternalResource(account, i));
    }

    public Optional<InstanceItem> describeRocket(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeRocketInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, InstanceItem rocket) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rocket.getInstanceId(),
                rocket.getInstanceName(),
                convertState(rocket.getInstanceStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status){
            case "CREATING" -> ResourceState.BUILDING;
            case "RUNNING" -> ResourceState.STARTED;
            case "DELETING" -> ResourceState.DESTROYING;
            case "DESTROYED" -> ResourceState.DESTROYED;
            case "OVERDUE" -> ResourceState.SHUTDOWN;
            case "MAINTAINING", "MODIFYING" -> ResourceState.CONFIGURING;
            case "CREATE_FAILURE" -> ResourceState.BUILD_ERROR;
            case "ABNORMAL", "MODIFY_FAILURE" -> ResourceState.ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeRocketInstances(
                new DescribeInstanceListRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceItem rocket = describeRocket(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Rocket not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, rocket));

        DescribeInstanceResponse detail = provider.buildClient(account).describeRocketInstanceDetail(
                resource.getExternalId()
        ).orElseThrow(
                () -> new ExternalResourceNotFoundException("Rocket not found: " + resource.getExternalId())
        );

        if(Utils.isNotEmpty(detail.getEndpointList())){
            for (int i = 0; i < detail.getEndpointList().length; i++) {
                RuntimeProperty endpointProperty = RuntimeProperty.ofDisplayInList(
                        "endpoint" + (i+1),
                        "接入点" + (i+1),
                        detail.getEndpointList()[i].getEndpointUrl(),
                        detail.getEndpointList()[i].getEndpointUrl()
                );
                resource.addOrUpdateRuntimeProperty(endpointProperty);
            }
        }

        boolean scaledTpsEnabled = detail.getScaledTpsEnabled() != null && detail.getScaledTpsEnabled();
        if(scaledTpsEnabled){
            RuntimeProperty scaledTpsLimitProperty = RuntimeProperty.ofDisplayInList(
                    "scaledTpsLimit",
                    "弹性TPS限流值",
                    String.valueOf(detail.getScaledTpsLimit()),
                    String.valueOf(detail.getScaledTpsLimit())
            );
            resource.addOrUpdateRuntimeProperty(scaledTpsLimitProperty);
        } else {
            RuntimeProperty tpsLimitProperty = RuntimeProperty.ofDisplayInList(
                    "tpsLimit",
                    "TPS限流值",
                    String.valueOf(detail.getTpsLimit()),
                    String.valueOf(detail.getTpsLimit())
            );
            resource.addOrUpdateRuntimeProperty(tpsLimitProperty);
        }
    }


    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

}
