package com.stratocloud.provider.aliyun.nic;

import com.aliyun.ecs20140526.models.DescribeEniMonitorDataRequest;
import com.aliyun.ecs20140526.models.DescribeNetworkInterfacesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.monitor.MonitoredResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunNicHandler extends AbstractResourceHandler implements MonitoredResourceHandler {

    private final AliyunCloudProvider provider;

    private final IpAllocator ipAllocator;


    public AliyunNicHandler(AliyunCloudProvider provider, IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_NIC";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云弹性网卡";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NIC;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeNic(account, externalId).map(
                nic -> toExternalResource(account, nic)
        );
    }

    public Optional<AliyunNic> describeNic(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeNic(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunNic nic) {
        String networkInterfaceId = nic.detail().getNetworkInterfaceId();
        String networkInterfaceName = nic.detail().getNetworkInterfaceName();

        String resourceName = Utils.isNotBlank(networkInterfaceName) ? networkInterfaceName : networkInterfaceId;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                networkInterfaceId,
                resourceName,
                convertState(nic.detail().getStatus())
        );
    }

    private ResourceState convertState(String status) {
        return switch (status){
            case "Available" -> ResourceState.IDLE;
            case "InUse" -> ResourceState.IN_USE;
            case "Attaching" -> ResourceState.ATTACHING;
            case "Detaching" -> ResourceState.DETACHING;
            case "Deleting" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        return client.ecs().describeNics(request).stream().map(nic -> toExternalResource(account, nic)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunNic> nic = describeNic(account, externalResource.externalId());

        if(nic.isEmpty())
            return List.of();

        var tags = nic.get().detail().getTags();
        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getTagKey(), tag.getTagKey()), tag.getTagValue(), tag.getTagValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunNic nic = describeNic(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Nic not found: " + resource.getName())
        );

        Optional<Resource> subnet = resource.getEssentialTarget(ResourceCategories.SUBNET);

        resource.updateByExternal(toExternalResource(account, nic));

        int ipCount = 0;


        List<String> ipList = nic.getPrivateIps();

        if(!ipList.isEmpty()){
            String ips = String.join(",", ipList);

            RuntimeProperty ipsProperty = RuntimeProperty.ofDisplayInList(
                    "ips", "IP地址(IPv4)", ips, ips
            );

            resource.addOrUpdateRuntimeProperty(ipsProperty);

            ipCount = ipCount + ipList.size();

            subnet.ifPresent(value -> ipAllocator.forceAllocateIps(value, InternetProtocol.IPv4, ipList, resource));
        }


        List<String> ipv6IpList = nic.getIpv6List();

        if(!ipv6IpList.isEmpty()){
            String ipv6Ips = String.join(",", ipv6IpList);
            RuntimeProperty ipv6IpsProperty = RuntimeProperty.ofDisplayInList(
                    "ipv6Ips", "IP地址(IPv6)", ipv6Ips, ipv6Ips
            );

            resource.addOrUpdateRuntimeProperty(ipv6IpsProperty);

            ipCount = ipCount + ipv6IpList.size();

            subnet.ifPresent(value -> ipAllocator.forceAllocateIps(value, InternetProtocol.IPv6, ipv6IpList, resource));
        }



        RuntimeProperty macProperty = RuntimeProperty.ofDisplayable(
                "macAddress", "MAC地址", nic.detail().getMacAddress(), nic.detail().getMacAddress()
        );
        resource.addOrUpdateRuntimeProperty(macProperty);

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayable(
                "type", "主网卡/辅助网卡", nic.detail().getType(), nic.detail().getType()
        );
        resource.addOrUpdateRuntimeProperty(typeProperty);

        var publicIp = nic.detail().getAssociatedPublicIp();

        if(publicIp != null && Utils.isNotBlank(publicIp.getPublicIpAddress())){
            RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                    "publicIp", "公网IP", publicIp.getPublicIpAddress(), publicIp.getPublicIpAddress()
            );

            resource.addOrUpdateRuntimeProperty(publicIpProperty);
        }

        resource.updateUsageByType(
                UsageTypes.NIC_IP, BigDecimal.valueOf(ipCount)
        );
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.NIC_IP);
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }

    @Override
    public Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index) {
        return ResourcePropertiesUtil.getPropertiesAtIndex(properties, index, List.of("ips"));
    }

    @Override
    public Optional<ResourceQuickStats> describeQuickStats(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunNic> aliyunNic = describeNic(account, resource.getExternalId());

        if(aliyunNic.isEmpty())
            return Optional.empty();

        var nic = aliyunNic.get().detail();

        if(Utils.isBlank(nic.getInstanceId()))
            return Optional.empty();

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(Duration.ofSeconds(180L));



        DescribeEniMonitorDataRequest request = new DescribeEniMonitorDataRequest();
        request.setStartTime(AliyunTimeUtil.toAliyunDateTime(startTime));
        request.setEndTime(AliyunTimeUtil.toAliyunDateTime(endTime));
        request.setEniId(nic.getNetworkInterfaceId());
        request.setInstanceId(nic.getInstanceId());
        request.setPeriod(60);

        var responseBody = provider.buildClient(account).ecs().describeNicMonitorData(request);

        if(Utils.isEmpty(responseBody.getMonitorData().getEniMonitorData()))
            return Optional.empty();

        var dataPoint = responseBody.getMonitorData().getEniMonitorData().get(0);

        String intranetRx = dataPoint.getIntranetRx();
        String intranetTx = dataPoint.getIntranetTx();

        ResourceQuickStats.Builder builder = ResourceQuickStats.builder();
        builder.addItem(
                "in", "接收速率", Double.parseDouble(intranetRx), "KBps"
        );
        builder.addItem(
                "out", "发送速率", Double.parseDouble(intranetTx), "KBps"
        );
        return Optional.of(builder.build());
    }
}
