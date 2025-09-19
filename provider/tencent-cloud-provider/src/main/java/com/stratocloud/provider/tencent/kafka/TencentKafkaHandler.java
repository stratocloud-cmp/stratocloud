package com.stratocloud.provider.tencent.kafka;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.ckafka.v20190819.models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentKafkaHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    private final IpAllocator ipAllocator;

    public TencentKafkaHandler(TencentCloudProvider provider,
                               IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_KAFKA";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云Kafka实例";
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
        Optional<InstanceDetail> kafka = describeKafka(account, externalId);

        return kafka.map(i -> toExternalResource(account, i));
    }

    public Optional<InstanceDetail> describeKafka(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeKafkaInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, InstanceDetail kafka) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                kafka.getInstanceId(),
                kafka.getInstanceName(),
                convertState(kafka.getStatus(), kafka.getHealthy())
        );
    }

    private ResourceState convertState(Long status, Long healthy) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status.intValue()){
            case 0 -> ResourceState.BUILDING;
            case 1 -> {
                if(Objects.equals(healthy, 1L))
                    yield ResourceState.HEALTH_CHECK_NORMAL;
                else if(Objects.equals(healthy, 2L))
                    yield ResourceState.HEALTH_CHECK_ABNORMAL;
                else
                    yield ResourceState.ERROR;
            }
            case 2 -> ResourceState.DESTROYING;
            case 3 -> ResourceState.DESTROYED;
            case 4 -> ResourceState.SHUTDOWN;
            case 5 -> ResourceState.CONFIGURING;
            case -1 -> ResourceState.BUILD_ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeKafkaInstances(
                new DescribeInstancesDetailRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceDetail kafka = describeKafka(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Kafka not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, kafka));

        if(Utils.isNotBlank(kafka.getVip())){
            RuntimeProperty ipProperty = RuntimeProperty.ofDisplayInList(
                    "vip",
                    "IP地址",
                    kafka.getVip(),
                    kafka.getVip()
            );
            resource.addOrUpdateRuntimeProperty(ipProperty);

            resource.getEssentialTarget(
                    ResourceCategories.SUBNET
            ).ifPresent(
                    s -> ipAllocator.forceAllocateIps(
                            s, InternetProtocol.IPv4, List.of(kafka.getVip()), resource
                    )
            );
        }

        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port",
                "端口",
                kafka.getVport(),
                kafka.getVport()
        );
        resource.addOrUpdateRuntimeProperty(portProperty);

        RuntimeProperty bandwidthProperty = RuntimeProperty.ofDisplayInList(
                "bandwidth",
                "内网带宽(Mbps)",
                String.valueOf(kafka.getBandwidth()),
                String.valueOf(kafka.getBandwidth())
        );
        resource.addOrUpdateRuntimeProperty(bandwidthProperty);

        RuntimeProperty publicBandwidthProperty = RuntimeProperty.ofDisplayable(
                "publicBandwidth",
                "公网带宽(Mbps)",
                String.valueOf(kafka.getPublicNetwork()),
                String.valueOf(kafka.getPublicNetwork())
        );
        resource.addOrUpdateRuntimeProperty(publicBandwidthProperty);

        RuntimeProperty diskTypeProperty = RuntimeProperty.ofDisplayable(
                "diskType",
                "磁盘类型",
                kafka.getDiskType(),
                kafka.getDiskType()
        );
        resource.addOrUpdateRuntimeProperty(diskTypeProperty);

        RuntimeProperty diskSizeProperty = RuntimeProperty.ofDisplayable(
                "diskSize",
                "磁盘大小(GB)",
                String.valueOf(kafka.getDiskSize()),
                String.valueOf(kafka.getDiskSize())
        );
        resource.addOrUpdateRuntimeProperty(diskSizeProperty);

        resource.updateUsageByType(
                UsageTypes.DISK_GB,
                BigDecimal.valueOf(kafka.getDiskSize())
        );
    }


    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.DISK_GB);
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceDetail> kafka = describeKafka(account, resource.getExternalId());

        var kafkaAttributes = client.describeKafkaAttributes(resource.getExternalId());

        if(kafka.isEmpty() || kafkaAttributes.isEmpty())
            return ResourceCost.ZERO;

        InquireCkafkaPriceRequest request = new InquireCkafkaPriceRequest();
        request.setInstanceType("profession");

        request.setInstanceNum(1L);
        request.setBandwidth(kafka.get().getBandwidth()/8);

        InquiryDiskParam diskParam = new InquiryDiskParam();
        diskParam.setDiskType(kafka.get().getDiskType());
        diskParam.setDiskSize(kafka.get().getDiskSize());
        request.setInquiryDiskParam(diskParam);

        Long retentionTime = kafkaAttributes.get().getMsgRetentionTime();
        request.setMessageRetention(retentionTime != null ? retentionTime/60 : null);
        request.setTopic(kafka.get().getMaxTopicNumber());
        request.setPartition(kafka.get().getMaxPartitionNumber());
        request.setZoneIds(kafka.get().getZoneIds());

        Long publicNetwork = kafka.get().getPublicNetwork();
        InquiryPublicNetworkParam publicNetworkParam = new InquiryPublicNetworkParam();
        publicNetworkParam.setPublicNetworkChargeType("BANDWIDTH_PREPAID");
        publicNetworkParam.setPublicNetworkMonthly(publicNetwork != null ? publicNetwork-3:0L);
        request.setPublicNetworkParam(publicNetworkParam);


        var response = client.describeKafkaPrice(request);

        if(response.getResult() == null)
            return ResourceCost.ZERO;

        if(response.getResult().getInstancePrice() == null)
            return ResourceCost.ZERO;

        Float instancePrice = response.getResult().getInstancePrice().getDiscountPrice();
        if(instancePrice == null)
            return ResourceCost.ZERO;

        double timeAmount = 1.0;
        ChronoUnit timeUnit = ChronoUnit.MONTHS;

        ResourceCost cost = new ResourceCost(instancePrice, timeAmount, timeUnit);

        InquiryPrice publicNetworkBandwidthPrice = response.getResult().getPublicNetworkBandwidthPrice();
        if(publicNetworkBandwidthPrice != null){
            Float publicNetworkDiscountPrice = publicNetworkBandwidthPrice.getDiscountPrice();

            if(publicNetworkDiscountPrice != null){
                cost = cost.add(
                        new ResourceCost(
                                publicNetworkDiscountPrice,
                                1.0,
                                ChronoUnit.MONTHS
                        )
                );
            }
        }

        return cost;
    }

}
