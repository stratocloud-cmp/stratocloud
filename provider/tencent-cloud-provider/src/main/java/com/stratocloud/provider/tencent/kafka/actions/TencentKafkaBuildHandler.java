package com.stratocloud.provider.tencent.kafka.actions;

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
import com.stratocloud.provider.tencent.kafka.TencentKafkaHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.ckafka.v20190819.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentKafkaBuildHandler implements BuildResourceActionHandler {

    private final TencentKafkaHandler kafkaHandler;

    public TencentKafkaBuildHandler(TencentKafkaHandler kafkaHandler) {
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
        return TencentKafkaBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) kafkaHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        List<ZoneInfo> zoneInfos = client.describeKafkaZones();

        List<ZoneInfo> availableZones = zoneInfos.stream().filter(
                z -> Objects.equals(z.getSoldOut(), "false")
        ).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentKafkaBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "zoneIds",
                availableZones.stream().map(ZoneInfo::getZoneId).toList(),
                availableZones.stream().map(ZoneInfo::getZoneName).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) kafkaHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentKafkaBuildInput input = JSON.convert(parameters, TencentKafkaBuildInput.class);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        String zone = subnet.getZone();

        var zoneInfo = client.describeZone(zone).orElseThrow(
                () -> new StratoException("Zone not found")
        );

        Long masterZoneId = Long.valueOf(zoneInfo.getZoneId());

        List<Long> zoneIds = new ArrayList<>();
        zoneIds.add(masterZoneId);

        if(input.isMultiZone() && Utils.isNotEmpty(input.getZoneIds())){
            for (Long zoneId : input.getZoneIds()) {
                if(!zoneIds.contains(zoneId))
                    zoneIds.add(zoneId);
            }
            zoneIds = new ArrayList<>(zoneIds.subList(0, 4));
        }

        if(Objects.equals(input.getPayType(), "PREPAID")){
            CreateInstancePreRequest request = new CreateInstancePreRequest();
            request.setInstanceName(resource.getName());
            request.setZoneId(masterZoneId);
            request.setPeriod(input.getPeriod());
            request.setInstanceType(1L);
            request.setVpcId(subnet.getVpcId());
            request.setSubnetId(subnet.getSubnetId());
            request.setMsgRetentionTime(input.getMsgRetentionTime() * 60);
            request.setRenewFlag(input.getAutoRenewFlag());
            request.setKafkaVersion(input.getKafkaVersion());
            request.setSpecificationsType("profession");

            request.setDiskType(input.getDiskType());
            if(input.getBandwidthType() == TencentKafkaBuildInput.BandwidthType.BIG){
                request.setDiskSize(input.getDiskSizeBig());
                request.setBandWidth(input.getBandwidthBig());
            } else {
                request.setDiskSize(input.getDiskSizeSmall());
                request.setBandWidth(input.getBandwidthSmall());
            }

            request.setPartition(input.getPartition());

            if(input.isMultiZone() && zoneIds.size() > 1){
                request.setMultiZoneFlag(true);
                request.setZoneIds(zoneIds.toArray(Long[]::new));
            } else {
                request.setMultiZoneFlag(false);
            }

            request.setPublicNetworkMonthly(input.getPublicNetworkMonthly()-3);
            request.setElasticBandwidthSwitch(input.getElasticBandwidthSwitch());

            request.setInstanceNum(1L);

            String instanceId = client.createKafkaPrepaidInstance(request);
            resource.setExternalId(instanceId);
        } else {
            CreatePostPaidInstanceRequest request = new CreatePostPaidInstanceRequest();

            request.setInstanceName(resource.getName());

            request.setZoneId(masterZoneId);
            request.setVpcId(subnet.getVpcId());
            request.setSubnetId(subnet.getSubnetId());
            request.setInstanceType(1L);
            request.setMsgRetentionTime(input.getMsgRetentionTime() * 60);

            request.setKafkaVersion(input.getKafkaVersion());
            request.setSpecificationsType("profession");
            request.setDiskType(input.getDiskType());

            if(input.getBandwidthType() == TencentKafkaBuildInput.BandwidthType.BIG){
                request.setDiskSize(input.getDiskSizeBig());
                request.setBandWidth(input.getBandwidthBig());
            } else {
                request.setDiskSize(input.getDiskSizeSmall());
                request.setBandWidth(input.getBandwidthSmall());
            }

            request.setPartition(input.getPartition());
            request.setTopicNum(input.getTopicNum());

            if(input.isMultiZone() && zoneIds.size() > 1){
                request.setMultiZoneFlag(true);
                request.setZoneIds(zoneIds.toArray(Long[]::new));
            } else {
                request.setMultiZoneFlag(false);
            }

            request.setPublicNetworkMonthly(input.getPublicNetworkMonthly()-3);
            request.setElasticBandwidthSwitch(input.getElasticBandwidthSwitch());

            request.setInstanceNum(1L);

            String instanceId = client.createKafkaPostpaidInstance(request);
            resource.setExternalId(instanceId);
        }
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) kafkaHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentKafkaBuildInput input = JSON.convert(parameters, TencentKafkaBuildInput.class);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        String zone = subnet.getZone();

        var zoneInfo = client.describeZone(zone).orElseThrow(
                () -> new StratoException("Zone not found")
        );

        Long masterZoneId = Long.valueOf(zoneInfo.getZoneId());

        List<Long> zoneIds = new ArrayList<>();
        zoneIds.add(masterZoneId);

        if(input.isMultiZone() && Utils.isNotEmpty(input.getZoneIds())){
            for (Long zoneId : input.getZoneIds()) {
                if(!zoneIds.contains(zoneId))
                    zoneIds.add(zoneId);
            }
            zoneIds = new ArrayList<>(zoneIds.subList(0, 4));
        }


        InquireCkafkaPriceRequest request = new InquireCkafkaPriceRequest();
        request.setInstanceType("profession");

        request.setInstanceNum(1L);

        InstanceChargeParam chargeParam = new InstanceChargeParam();
        chargeParam.setInstanceChargeType(
                Objects.equals(input.getPayType(), "POSTPAID") ?
                        "POSTPAID_BY_HOUR" : "PREPAID"
        );
        chargeParam.setInstanceChargePeriod(
                Objects.equals(input.getPayType(), "POSTPAID") ?
                null : Long.parseLong(input.getPeriod().replace("m", ""))
        );
        request.setInstanceChargeParam(chargeParam);

        request.setBandwidth(
                input.getBandwidthType() == TencentKafkaBuildInput.BandwidthType.BIG ?
                        input.getBandwidthBig() : input.getBandwidthSmall()
        );

        InquiryDiskParam diskParam = new InquiryDiskParam();
        diskParam.setDiskType(input.getDiskType());
        diskParam.setDiskSize(
                input.getBandwidthType() == TencentKafkaBuildInput.BandwidthType.BIG ?
                        input.getDiskSizeBig() : input.getDiskSizeSmall()
        );
        request.setInquiryDiskParam(diskParam);

        Long retentionTime = input.getMsgRetentionTime();
        request.setMessageRetention(retentionTime);
        request.setTopic(Objects.equals(input.getPayType(), "POSTPAID") ? input.getTopicNum() : null);
        request.setPartition(input.getPartition());

        request.setZoneIds(zoneIds.toArray(Long[]::new));

        InquiryPublicNetworkParam publicNetworkParam = new InquiryPublicNetworkParam();
        publicNetworkParam.setPublicNetworkChargeType(
                Objects.equals(input.getPayType(), "POSTPAID") ?
                        "BANDWIDTH_POSTPAID_BY_HOUR" : "BANDWIDTH_PREPAID"
        );
        publicNetworkParam.setPublicNetworkMonthly(input.getPublicNetworkMonthly()-3);
        request.setPublicNetworkParam(publicNetworkParam);


        var response = client.describeKafkaPrice(request);

        if(response.getResult() == null)
            return ResourceCost.ZERO;

        if(response.getResult().getInstancePrice() == null)
            return ResourceCost.ZERO;

        Float instancePrice = response.getResult().getInstancePrice().getDiscountPrice();
        if(instancePrice == null)
            return ResourceCost.ZERO;

        double timeAmount;
        ChronoUnit timeUnit;

        if(Objects.equals(input.getPayType(), "PREPAID")){
            timeAmount = Double.parseDouble(input.getPeriod().replace("m", ""));
            timeUnit = ChronoUnit.MONTHS;
        }else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;
        }

        ResourceCost cost = new ResourceCost(instancePrice, timeAmount, timeUnit);

        InquiryPrice publicNetworkBandwidthPrice = response.getResult().getPublicNetworkBandwidthPrice();
        if(publicNetworkBandwidthPrice != null){
            Float publicNetworkDiscountPrice = publicNetworkBandwidthPrice.getDiscountPrice();

            if(publicNetworkDiscountPrice != null){
                cost = cost.add(
                        new ResourceCost(
                                publicNetworkDiscountPrice,
                                timeAmount,
                                timeUnit
                        )
                );
            }
        }

        return cost;
    }
}
