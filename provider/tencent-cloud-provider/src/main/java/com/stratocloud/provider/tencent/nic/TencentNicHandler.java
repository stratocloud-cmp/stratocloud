package com.stratocloud.provider.tencent.nic;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.CompareUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.DescribeNetworkInterfacesRequest;
import com.tencentcloudapi.vpc.v20170312.models.Ipv6Address;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import com.tencentcloudapi.vpc.v20170312.models.PrivateIpAddressSpecification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    private final IpAllocator ipAllocator;


    public TencentNicHandler(TencentCloudProvider provider, IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_NIC";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云弹性网卡";
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

    public Optional<NetworkInterface> describeNic(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeNic(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, NetworkInterface nic) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                nic.getNetworkInterfaceId(),
                nic.getNetworkInterfaceName(),
                convertState(nic)
        );
    }

    private ResourceState convertState(NetworkInterface nic) {
        boolean attached = nic.getAttachment() != null && Utils.isNotBlank(nic.getAttachment().getInstanceId());

        return switch (nic.getState()){
            case "AVAILABLE" -> attached ? ResourceState.IN_USE : ResourceState.IDLE;
            case "PENDING" -> ResourceState.BUILDING;
            case "ATTACHING" -> ResourceState.ATTACHING;
            case "DETACHING" -> ResourceState.DETACHING;
            case "DELETING" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        return client.describeNics(request).stream().map(nic -> toExternalResource(account, nic)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<NetworkInterface> nic = describeNic(account, externalResource.externalId());

        if(nic.isEmpty())
            return List.of();

        if(nic.get().getTagSet() == null)
            return List.of();

        return Arrays.stream(nic.get().getTagSet()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        NetworkInterface nic = describeNic(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Nic not found: " + resource.getName())
        );



        resource.updateByExternal(toExternalResource(account, nic));


        List<String> ipList = getIps(nic.getPrivateIpAddressSet());
        String ips = String.join(",", ipList);

        RuntimeProperty ipsProperty = RuntimeProperty.ofDisplayInList(
                "ips", "IP地址(IPv4)", ips, ips
        );

        List<String> ipv6IpList = getIps(nic.getIpv6AddressSet());
        String ipv6Ips = String.join(",", ipv6IpList);
        RuntimeProperty ipv6IpsProperty = RuntimeProperty.ofDisplayInList(
                "ipv6Ips", "IP地址(IPv6)", ipv6Ips, ipv6Ips
        );

        RuntimeProperty macProperty = RuntimeProperty.ofDisplayable(
                "macAddress", "MAC地址", nic.getMacAddress(), nic.getMacAddress()
        );

        String primary = String.valueOf(nic.getPrimary());
        RuntimeProperty primaryProperty = RuntimeProperty.ofDisplayable(
                "primary", "是否为主网卡", primary, primary
        );

        String publicIps = String.join(",", getPublicIps(nic.getPrivateIpAddressSet()));
        RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                "publicIp", "公网IP", publicIps, publicIps
        );

        resource.addOrUpdateRuntimeProperty(ipsProperty);
        resource.addOrUpdateRuntimeProperty(ipv6IpsProperty);
        resource.addOrUpdateRuntimeProperty(macProperty);
        resource.addOrUpdateRuntimeProperty(primaryProperty);
        resource.addOrUpdateRuntimeProperty(publicIpProperty);

        resource.updateUsageByType(UsageTypes.NIC_IP, BigDecimal.valueOf(ipList.size()+ipv6IpList.size()));

        Optional<Resource> subnet = resource.getEssentialTarget(ResourceCategories.SUBNET);

        subnet.ifPresent(value -> ipAllocator.forceAllocateIps(value, InternetProtocol.IPv4, ipList, resource));
        subnet.ifPresent(value -> ipAllocator.forceAllocateIps(value, InternetProtocol.IPv6, ipv6IpList, resource));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.NIC_IP);
    }


    public List<String> getIps(Ipv6Address[] addresses) {
        if(Utils.isEmpty(addresses))
            return List.of();

        return Arrays.stream(addresses).sorted(
                (ip1, ip2) -> CompareUtil.compareBooleanDesc(ip1.getPrimary(), ip2.getPrimary())
        ).map(Ipv6Address::getAddress).toList();
    }

    public List<String> getIps(PrivateIpAddressSpecification[] addressSet) {
        if(Utils.isEmpty(addressSet))
            return List.of();

        return Arrays.stream(addressSet).sorted(
                (ip1, ip2) -> CompareUtil.compareBooleanDesc(ip1.getPrimary(), ip2.getPrimary())
        ).map(
                PrivateIpAddressSpecification::getPrivateIpAddress
        ).toList();
    }

    public List<String> getPublicIps(PrivateIpAddressSpecification[] addressSet) {
        if(Utils.isEmpty(addressSet))
            return List.of();

        return Arrays.stream(addressSet).filter(ip -> Utils.isNotBlank(ip.getPublicIpAddress())).sorted(
                (ip1, ip2) -> CompareUtil.compareBooleanDesc(ip1.getPrimary(), ip2.getPrimary())
        ).map(
                PrivateIpAddressSpecification::getPublicIpAddress
        ).toList();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }


    @Override
    public Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index) {
        return ResourcePropertiesUtil.getPropertiesAtIndex(properties, index, List.of("ips"));
    }
}
