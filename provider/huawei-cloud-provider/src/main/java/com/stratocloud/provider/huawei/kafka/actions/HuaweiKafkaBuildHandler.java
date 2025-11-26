package com.stratocloud.provider.huawei.kafka.actions;

import com.huaweicloud.sdk.kafka.v2.model.AvailableZonesResp;
import com.huaweicloud.sdk.kafka.v2.model.BssParam;
import com.huaweicloud.sdk.kafka.v2.model.CreateInstanceByEngineReq;
import com.huaweicloud.sdk.kafka.v2.model.CreatePostPaidKafkaInstanceRequest;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.kafka.HuaweiKafkaHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HuaweiKafkaBuildHandler implements BuildResourceActionHandler {

    private final HuaweiKafkaHandler kafkaHandler;

    public HuaweiKafkaBuildHandler(HuaweiKafkaHandler kafkaHandler) {
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
        return HuaweiKafkaBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) kafkaHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        return Optional.of(HuaweiKafkaBuildInput.getFormMeta(client));
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiKafkaBuildInput input = JSON.convert(parameters, HuaweiKafkaBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) kafkaHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);



        CreatePostPaidKafkaInstanceRequest request = new CreatePostPaidKafkaInstanceRequest();
        CreateInstanceByEngineReq body = new CreateInstanceByEngineReq();
        body.setArchType("X86");
        body.setName(resource.getName());
        body.setDescription(resource.getDescription());

        body.setEngine(CreateInstanceByEngineReq.EngineEnum.KAFKA);
        body.setEngineVersion(input.getEngineVersion());
        body.setBrokerNum(input.getBrokerNumber());
        body.setProductId(input.getProductId());
        body.setStorageSpecCode(CreateInstanceByEngineReq.StorageSpecCodeEnum.fromValue(input.getStorageSpecCode()));
        body.setStorageSpace(input.getStorageSpace() * input.getBrokerNumber());

        resolvePlacement(resource, client, body, input);

        body.setRetentionPolicy(CreateInstanceByEngineReq.RetentionPolicyEnum.fromValue(input.getRetentionPolicy()));
        body.setEnableAutoTopic(input.isEnableAutoTopic());



        BssParam bssParam = new BssParam();
        bssParam.setIsAutoPay(true);
        bssParam.setChargingMode(BssParam.ChargingModeEnum.fromValue(input.getChargingMode()));
        if(Objects.equals(input.getChargingMode(), "prePaid")){
            bssParam.setIsAutoRenew(input.isAutoRenew());
            if(input.getPeriod() >= 12L){
                bssParam.setPeriodType(BssParam.PeriodTypeEnum.YEAR);
                bssParam.setPeriodNum(input.getPeriod().intValue()/12);
            }else {
                bssParam.setPeriodType(BssParam.PeriodTypeEnum.MONTH);
                bssParam.setPeriodNum(input.getPeriod().intValue());
            }
        }
        body.setBssParam(bssParam);

        request.setBody(body);

        String instanceId = client.kafka().createInstance(request);
        resource.setExternalId(instanceId);
    }

    private static void resolvePlacement(Resource resource,
                                         HuaweiCloudClient client,
                                         CreateInstanceByEngineReq body,
                                         HuaweiKafkaBuildInput input) {
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        Resource securityGroupResource = resource.getEssentialTarget(ResourceCategories.SECURITY_GROUP).orElseThrow(
                () -> new StratoException("Security group not provided")
        );

        Map<String, AvailableZonesResp> azMap = client.kafka().describeZones().stream().collect(
                Collectors.toMap(
                        AvailableZonesResp::getCode,
                        z -> z
                )
        );

        body.setVpcId(subnet.getVpcId());
        body.setSecurityGroupId(securityGroupResource.getExternalId());
        body.setSubnetId(subnet.getId());

        List<String> azCodes = new ArrayList<>();
        azCodes.add(subnet.getAvailabilityZone());

        if(input.isEnableBackupZones()){
            azCodes.add(input.getFirstBackupZone());
            azCodes.add(input.getSecondBackupZone());
        }

        List<String> azIds = azCodes.stream().map(c -> azMap.get(c).getId()).toList();

        body.setAvailableZones(azIds);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
