package com.stratocloud.provider.huawei.nic;

import com.huaweicloud.sdk.vpc.v2.model.FixedIp;
import com.huaweicloud.sdk.vpc.v2.model.ListPortsRequest;
import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class HuaweiNicHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    private final IpAllocator ipAllocator;

    public HuaweiNicHandler(HuaweiCloudProvider provider, IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_NIC";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云网卡";
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
        return describePort(account, externalId).map(
                port -> toExternalResource(account, port)
        );
    }

    public Optional<Port> describePort(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describePort(externalId).filter(
                HuaweiNicHelper::filterOutRouterInterfaceAndDhcpAgent
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, Port port) {
        String name;

        if(Utils.isNotBlank(port.getName()))
            name = port.getName();
        else if(Utils.isNotEmpty(port.getFixedIps()))
            name = port.getFixedIps().get(0).getIpAddress();
        else
            name = port.getMacAddress();


        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                port.getId(),
                name,
                convertState(port.getStatus())
        );
    }

    private ResourceState convertState(Port.StatusEnum state) {
        if(Objects.equals(Port.StatusEnum.ACTIVE, state))
            return ResourceState.STARTED;
        else if(Objects.equals(Port.StatusEnum.DOWN, state))
            return ResourceState.STOPPED;
        else if(Objects.equals(Port.StatusEnum.BUILD, state))
            return ResourceState.BUILDING;
        else
            return ResourceState.UNKNOWN;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        var client = provider.buildClient(account);
        return client.vpc().describePorts(new ListPortsRequest()).stream().filter(
                HuaweiNicHelper::filterOutRouterInterfaceAndDhcpAgent
        ).map(
                port -> toExternalResource(account, port)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Port port = describePort(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Port not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, port));


        if(Utils.isNotEmpty(port.getFixedIps())){
            List<String> ips = port.getFixedIps().stream().map(FixedIp::getIpAddress).toList();
            RuntimeProperty ipsProperty = RuntimeProperty.ofDisplayInList(
                    "ips", "IP地址", String.join(",", ips), String.join(",", ips)
            );
            resource.addOrUpdateRuntimeProperty(ipsProperty);

            resource.updateUsageByType(UsageTypes.NIC_IP, BigDecimal.valueOf(ips.size()));

            Optional<Resource> subnet = resource.getEssentialTarget(ResourceCategories.SUBNET);

            subnet.ifPresent(value -> ipAllocator.forceAllocateIps(value, InternetProtocol.IPv4, ips, resource));
        }

        if(Utils.isNotBlank(port.getMacAddress())){
            RuntimeProperty macProperty = RuntimeProperty.ofDisplayInList(
                    "macAddress", "MAC地址", port.getMacAddress(), port.getMacAddress()
            );
            resource.addOrUpdateRuntimeProperty(macProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.NIC_IP
        );
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
