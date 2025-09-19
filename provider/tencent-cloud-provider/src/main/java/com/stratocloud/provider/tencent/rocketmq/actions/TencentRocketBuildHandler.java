package com.stratocloud.provider.tencent.rocketmq.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.rocketmq.RocketUtil;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.region.v20220627.models.ZoneInfo;
import com.tencentcloudapi.trocket.v20230308.models.CreateInstanceRequest;
import com.tencentcloudapi.trocket.v20230308.models.ProductSKU;
import com.tencentcloudapi.trocket.v20230308.models.VpcInfo;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentRocketBuildHandler implements BuildResourceActionHandler {

    private final TencentRocketHandler rocketHandler;

    public TencentRocketBuildHandler(TencentRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RocketMQ实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentRocketBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        List<ZoneInfo> zoneInfos = client.describeRocketZones().stream().filter(
                z -> Objects.equals(z.getZoneState(), "AVAILABLE")
        ).toList();
        List<ProductSKU> skuList = client.describeRocketSkuList().stream().sorted(
                Comparator.comparing(RocketUtil::getSkuInstanceTypePriority).thenComparing(ProductSKU::getTpsLimit)
        ).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentRocketBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "backupZoneId",
                zoneInfos.stream().map(
                        ZoneInfo::getZoneId
                ).toList(),
                zoneInfos.stream().map(
                        ZoneInfo::getZoneName
                ).toList()
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "skuCode",
                skuList.stream().map(ProductSKU::getSkuCode).toList(),
                skuList.stream().map(RocketUtil::formatSkuName).toList()
        );


        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        TencentRocketBuildInput input = JSON.convert(parameters, TencentRocketBuildInput.class);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        com.tencentcloudapi.cvm.v20170312.models.ZoneInfo zoneInfo = client.describeZone(subnet.getZone()).orElseThrow(
                () -> new StratoException("Zone not found")
        );

        ProductSKU sku = client.describeRocketSku(input.getSkuCode()).orElseThrow(
                () -> new StratoException("RocketMQ sku not found")
        );

        CreateInstanceRequest request = new CreateInstanceRequest();

        request.setName(resource.getName());


        request.setPayMode(input.getPayMode());

        if(Objects.equals(input.getPayMode(), 1L)){
            request.setRenewFlag(input.getRenewFlag());
            request.setTimeSpan(input.getTimeSpan());
        }

        request.setInstanceType(sku.getInstanceType());
        request.setSkuCode(sku.getSkuCode());
        request.setMaxTopicNum(
                Math.min(sku.getTopicNumLimit()+ input.getExtraMaxTopicNum(), sku.getTopicNumUpperLimit())
        );

        if(Objects.equals(sku.getInstanceType(), "BASIC"))
            request.setMessageRetention(input.getMessageRetention());

        if(input.isEnableMultiZone() && Utils.isNotBlank(input.getBackupZoneId()))
            request.setZoneIds(
                    new Long[]{
                            Long.parseLong(zoneInfo.getZoneId()),
                            Long.parseLong(input.getBackupZoneId())
                    }
            );
        else
            request.setZoneIds(
                    new Long[]{
                            Long.parseLong(zoneInfo.getZoneId())
                    }
            );


        VpcInfo vpcInfo = new VpcInfo();
        vpcInfo.setVpcId(subnet.getVpcId());
        vpcInfo.setSubnetId(subnet.getSubnetId());
        request.setVpcList(new VpcInfo[]{vpcInfo});

        request.setEnablePublic(input.isEnablePublic());
        if(input.isEnablePublic()){
            request.setBillingFlow(input.isBillingFlow());
            request.setBandwidth(input.getBandwidth());
        }

        String instanceId = client.createRocketInstance(request);
        resource.setExternalId(instanceId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
