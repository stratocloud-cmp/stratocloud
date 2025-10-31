package com.stratocloud.provider.aliyun.kafka;

import com.aliyun.alikafka20190916.models.GetInstanceListRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunKafkaHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunKafkaHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_KAFKA_HANDLER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云Kafka";
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

    private ExternalResource toExternalResource(ExternalAccount account, KafkaInstance instance) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instance.detail().getInstanceId(),
                instance.detail().getName(),
                convertState(instance.detail().getViewInstanceStatusCode())
        );
    }

    private ResourceState convertState(Integer status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status) {
            case 0 -> ResourceState.NO_STATE;
            case 1 -> ResourceState.BUILDING;
            case 2, 4 -> ResourceState.STARTED;
            case 3 -> ResourceState.STOPPED;
            case 5, 6, 23 -> ResourceState.SHUTDOWN;
            case 7, 8, 30 -> ResourceState.CONFIGURING;
            case 21 -> ResourceState.STOPPING;
            case 22 -> ResourceState.STARTING;
            case 101 -> ResourceState.BUILD_ERROR;
            case 102, 103 -> ResourceState.ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<KafkaInstance> describeInstance(ExternalAccount account, String instanceId){
        if(Utils.isBlank(instanceId))
            return Optional.empty();

        return provider.buildClient(account).kafka().describeInstance(instanceId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).kafka().describeInstances(new GetInstanceListRequest()).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        KafkaInstance kafkaInstance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Kafka mq instance not found")
        );

        resource.updateByExternal(toExternalResource(account, kafkaInstance));

        String specType = kafkaInstance.detail().getSpecType();

        RuntimeProperty specTypeProperty = RuntimeProperty.ofDisplayInList(
                "specType",
                "规格类型",
                specType,
                getSpecName(specType)
        );

        resource.addOrUpdateRuntimeProperty(specTypeProperty);

        Integer usedTopicCount = kafkaInstance.detail().getUsedTopicCount();
        if(usedTopicCount==null)
            usedTopicCount = 0;
        RuntimeProperty topicCountProperty = RuntimeProperty.ofDisplayInList(
                "topicCount",
                "主题数量",
                "%s/%s".formatted(usedTopicCount, kafkaInstance.detail().getTopicNumLimit()),
                "%s/%s".formatted(usedTopicCount, kafkaInstance.detail().getTopicNumLimit())
        );
        resource.addOrUpdateRuntimeProperty(topicCountProperty);

        Integer usedGroupCount = kafkaInstance.detail().getUsedGroupCount();
        if(usedGroupCount==null)
            usedGroupCount = 0;
        RuntimeProperty groupCountProperty = RuntimeProperty.ofDisplayInList(
                "groupCount",
                "消费者组数量",
                "%s/%s".formatted(usedGroupCount, kafkaInstance.detail().getTopicNumLimit()*2),
                "%s/%s".formatted(usedGroupCount, kafkaInstance.detail().getTopicNumLimit()*2)
        );
        resource.addOrUpdateRuntimeProperty(groupCountProperty);

        Integer diskSize = kafkaInstance.detail().getDiskSize();

        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(diskSize));
    }

    private static String getSpecName(String specType) {
        if(Utils.isBlank(specType))
            return "Unknown";

        return switch (specType){
            case "basic" -> "基础版";
            case "normal" -> "标准版";
            case "professional" -> "专业版";
            case "professionalForHighRead" -> "专业版（高读版）";
            case "enterprise" -> "企业版";
            default -> specType;
        };
    }



    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.DISK_GB
        );
    }
}
