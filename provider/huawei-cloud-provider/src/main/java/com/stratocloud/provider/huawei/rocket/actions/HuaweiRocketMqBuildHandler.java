package com.stratocloud.provider.huawei.rocket.actions;

import com.huaweicloud.sdk.rocketmq.v2.model.*;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.rocket.HuaweiRocketMqHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HuaweiRocketMqBuildHandler implements BuildResourceActionHandler {

    private final HuaweiRocketMqHandler rocketMqHandler;

    public HuaweiRocketMqBuildHandler(HuaweiRocketMqHandler rocketMqHandler) {
        this.rocketMqHandler = rocketMqHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketMqHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RocketMQ实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiRocketMqBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rocketMqHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        return Optional.of(HuaweiRocketMqBuildInput.getFormMeta(client));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiRocketMqBuildInput input = JSON.convert(parameters, HuaweiRocketMqBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rocketMqHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        ProductEntity product = client.rocket().describeProduct(input.getProductId()).orElseThrow(
                () -> new StratoException("Product not found")
        );

        Map<String, Object> productProperties = (Map<String, Object>) product.getProperties();


        CreateInstanceByEngineRequest request = new CreateInstanceByEngineRequest();
        CreateInstanceByEngineReq body = new CreateInstanceByEngineReq();
        body.setArchType(input.getArchType());
        body.setName(resource.getName());
        body.setDescription(resource.getDescription());

        body.setEngine(CreateInstanceByEngineReq.EngineEnum.RELIABILITY);
        body.setEngineVersion(
                CreateInstanceByEngineReq.EngineVersionEnum.fromValue(
                        MapUtils.getString(productProperties, "engine_versions")
                )
        );

        if(Objects.equals(input.getInstanceType(), "cluster")){
            body.setBrokerNum(input.getBrokerNumber());
        }else {
            body.setBrokerNum(MapUtils.getInteger(productProperties, "broker_num"));
        }


        body.setProductId(CreateInstanceByEngineReq.ProductIdEnum.fromValue(input.getProductId()));
        body.setStorageSpecCode(CreateInstanceByEngineReq.StorageSpecCodeEnum.fromValue(input.getStorageSpecCode()));
        body.setStorageSpace(input.getStorageSpace() * input.getBrokerNumber());

        resolvePlacement(resource, client, body, input);

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

        String instanceId = client.rocket().createInstance(request);
        resource.setExternalId(instanceId);
    }

    private static void resolvePlacement(Resource resource,
                                         HuaweiCloudClient client,
                                         CreateInstanceByEngineReq body,
                                         HuaweiRocketMqBuildInput input) {
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        Resource securityGroupResource = resource.getEssentialTarget(ResourceCategories.SECURITY_GROUP).orElseThrow(
                () -> new StratoException("Security group not provided")
        );

        Map<String, ListAvailableZonesRespAvailableZones> azMap = client.rocket().describeZones().stream().collect(
                Collectors.toMap(
                        ListAvailableZonesRespAvailableZones::getCode,
                        z -> z
                )
        );

        body.setVpcId(subnet.getVpcId());
        body.setSecurityGroupId(securityGroupResource.getExternalId());
        body.setSubnetId(subnet.getId());

        List<String> azCodes = new ArrayList<>();
        azCodes.add(subnet.getAvailabilityZone());

        if(!Objects.equals(input.getInstanceType(), "single.basic") && Utils.isNotBlank(input.getBackupZone())){
            azCodes.add(input.getBackupZone());
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
