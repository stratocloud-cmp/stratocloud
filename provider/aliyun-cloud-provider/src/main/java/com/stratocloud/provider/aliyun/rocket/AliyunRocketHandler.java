package com.stratocloud.provider.aliyun.rocket;

import com.aliyun.rocketmq20220801.models.ListInstancesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunRocketHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunRocketHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_ROCKETMQ_HANDLER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云RocketMQ";
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
        return describeInstance(account, externalId).map(
                i -> toExternalResource(account, i)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, RocketInstance instance) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instance.detail().getInstanceId(),
                instance.detail().getInstanceName(),
                convertState(instance.detail().getStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;

        return switch (status) {
            case "RELEASED" -> ResourceState.DESTROYED;
            case "RUNNING" -> ResourceState.STARTED;
            case "STOPPED" -> ResourceState.STOPPED;
            case "CHANGING" -> ResourceState.CONFIGURING;
            case "CREATING" -> ResourceState.BUILDING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<RocketInstance> describeInstance(ExternalAccount account, String instanceId){
        if(Utils.isBlank(instanceId))
            return Optional.empty();

        return provider.buildClient(account).rocket().describeInstance(instanceId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).rocket().describeInstances(new ListInstancesRequest()).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        RocketInstance rocketInstance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Rocket mq instance not found")
        );

        resource.updateByExternal(toExternalResource(account, rocketInstance));

        String seriesCode = rocketInstance.detail().getSeriesCode();

        RuntimeProperty seriesProperty = RuntimeProperty.ofDisplayInList(
                "series",
                "实例主系列",
                seriesCode,
                getSeriesName(seriesCode)
        );

        resource.addOrUpdateRuntimeProperty(seriesProperty);

        String subSeriesCode = rocketInstance.detail().getSubSeriesCode();
        RuntimeProperty subSeriesProperty = RuntimeProperty.ofDisplayInList(
                "subSeries",
                "实例子系列",
                subSeriesCode,
                getSubSeriesName(subSeriesCode)
        );

        resource.addOrUpdateRuntimeProperty(subSeriesProperty);

        Long topicCount = rocketInstance.detail().getTopicCount();
        RuntimeProperty topicCountProperty = RuntimeProperty.ofDisplayInList(
                "topicCount",
                "主题数量",
                String.valueOf(topicCount),
                String.valueOf(topicCount)
        );
        resource.addOrUpdateRuntimeProperty(topicCountProperty);

        Long groupCount = rocketInstance.detail().getGroupCount();
        RuntimeProperty groupCountProperty = RuntimeProperty.ofDisplayInList(
                "groupCount",
                "消费者组数量",
                String.valueOf(groupCount),
                String.valueOf(groupCount)
        );
        resource.addOrUpdateRuntimeProperty(groupCountProperty);
    }

    private static String getSubSeriesName(String subSeriesCode) {
        if(Utils.isBlank(subSeriesCode))
            return "Unknown";

        return switch (subSeriesCode){
            case "cluster_ha" -> "集群高可用版";
            case "single_node" -> "单节点版";
            default -> subSeriesCode;
        };
    }

    private static String getSeriesName(String seriesCode) {
        if(Utils.isBlank(seriesCode))
            return "Unknown";

        return switch (seriesCode){
            case "standard" -> "标准版";
            case "ultimate" -> "铂金版";
            case "professional" -> "专业版";
            default -> seriesCode;
        };
    }



    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
