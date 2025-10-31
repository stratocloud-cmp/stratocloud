package com.stratocloud.provider.aliyun.kafka.actions;

import com.aliyun.alikafka20190916.models.CreatePostPayInstanceRequest;
import com.aliyun.alikafka20190916.models.CreatePrePayInstanceRequest;
import com.aliyun.alikafka20190916.models.StartInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.kafka.AliyunKafkaHandler;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.zone.AliyunZone;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AliyunKafkaBuildHandler implements BuildResourceActionHandler {

    private final AliyunKafkaHandler kafkaHandler;

    public AliyunKafkaBuildHandler(AliyunKafkaHandler kafkaHandler) {
        this.kafkaHandler = kafkaHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return kafkaHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Kafka实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunKafkaBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunKafkaBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunCloudProvider provider = (AliyunCloudProvider) kafkaHandler.getProvider();

        List<AliyunZone> zones = provider.buildClient(account).ecs().describeZones();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "backupZones",
                zones.stream().map(AliyunZone::getZoneId).toList(),
                zones.stream().map(z -> z.zone().getLocalName()).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunKafkaBuildInput input = JSON.convert(parameters, AliyunKafkaBuildInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) kafkaHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        if(Objects.equals(input.getPayType(), "prepaid")){
            CreatePrePayInstanceRequest request = new CreatePrePayInstanceRequest();

            request.setPaidType(0);
            request.setDuration(input.getPeriod().intValue());

            request.setDeployType(input.getDeployType().intValue());

            if(input.getDeployType() == 4L)
                request.setEipMax(input.getEipMax());

            request.setDiskType(input.getDiskType());
            request.setDiskSize(input.getDiskSize());

            request.setSpecType(input.getSpecType());
            request.setIoMaxSpec(getIoMaxSpec(input));

            if(input.getPartitionNum() != null && input.getPartitionNum() > 0)
                request.setPartitionNum(input.getPartitionNum());

            String instanceId = client.kafka().createPrePayInstance(request);
            resource.setExternalId(instanceId);
        } else {
            CreatePostPayInstanceRequest request = new CreatePostPayInstanceRequest();

            request.setPaidType(1);

            request.setDeployType(input.getDeployType().intValue());

            if(input.getDeployType() == 4L)
                request.setEipMax(input.getEipMax());

            request.setDiskType(input.getDiskType());
            request.setDiskSize(input.getDiskSize());

            request.setSpecType(input.getSpecType());
            request.setIoMaxSpec(getIoMaxSpec(input));

            if(input.getPartitionNum() != null && input.getPartitionNum() > 0)
                request.setPartitionNum(input.getPartitionNum());

            String instanceId = client.kafka().createPostPayInstance(request);
            resource.setExternalId(instanceId);
        }

        SleepUtil.sleep(15);

        try {
            deployKafkaInstance(resource, input, client);
        }catch (Exception e){
            log.error("Failed to deploy kafka instance", e);
        }
    }

    private void deployKafkaInstance(Resource resource,
                                     AliyunKafkaBuildInput input,
                                     AliyunClient client) {
        Resource zoneResource = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not provided")
        );
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );
        AliyunSubnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        StartInstanceRequest request = new StartInstanceRequest();

        request.setInstanceId(resource.getExternalId());
        request.setName(resource.getName());
        request.setDeployModule(input.getDeployType() == 4L ? "eip" : "vpc");
        request.setIsEipInner(input.getDeployType() == 4L);


        request.setZoneId(zoneResource.getExternalId());
        request.setVpcId(subnet.detail().getVpcId());
        request.setVSwitchId(subnet.detail().getVSwitchId());
        request.setVSwitchIds(List.of(subnet.detail().getVSwitchId()));
        request.setCrossZone(input.isCrossZone());
        if(input.isCrossZone()){
            request.setSelectedZones(
                    JSON.toJsonString(
                            List.of(
                                    convertZoneIds(List.of(zoneResource.getExternalId())),
                                    convertZoneIds(input.getBackupZones())
                            )
                    )
            );
        }else {
            request.setSelectedZones(
                    JSON.toJsonString(
                            List.of(
                                    convertZoneIds(List.of(zoneResource.getExternalId())),
                                    List.of()
                            )
                    )
            );
        }

        client.kafka().startInstance(request);
    }

    private List<String> convertZoneIds(List<String> zoneIds) {
        if(Utils.isEmpty(zoneIds))
            return List.of();
        return zoneIds.stream().map(
                this::convertZoneId
        ).toList();
    }

    private String convertZoneId(String zoneId) {
        String[] split = zoneId.split("-");
        return "zone" + split[split.length-1];
    }

    private String getIoMaxSpec(AliyunKafkaBuildInput input) {
        String specType = input.getSpecType();
        return switch (specType) {
            case "normal" ->  input.getNormalIoMaxSpec();
            case "professional" -> input.getProIoMaxSpec();
            case "professionalForHighRead" -> input.getHrIoMaxSpec();
            default -> null;
        };
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        AliyunKafkaBuildInput input = JSON.convert(parameters, AliyunKafkaBuildInput.class);
        return List.of(
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(),
                        BigDecimal.valueOf(input.getDiskSize())
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
