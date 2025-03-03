package com.stratocloud.provider.huawei.nic;

import com.huaweicloud.sdk.vpc.v2.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.nic.actions.HuaweiNicBuildInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.stratocloud.utils.concurrent.SlowTaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class HuaweiNicHelper {
    private final IpAllocator ipAllocator;

    public HuaweiNicHelper(IpAllocator ipAllocator) {
        this.ipAllocator = ipAllocator;
    }


    public static boolean isPrimaryInterface(Port port){
        BindingVifDetails vifDetails = port.getBindingVifDetails();
        return vifDetails != null && vifDetails.getPrimaryInterface() != null && vifDetails.getPrimaryInterface();
    }



    public void createPort(Resource resource, Map<String, Object> parameters) {
        HuaweiNicBuildInput input = JSON.convert(parameters, HuaweiNicBuildInput.class);
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided when creating port.")
        );

        List<Resource> securityGroups = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        ResourceHandler resourceHandler = resource.getResourceHandler();

        ExternalAccount account = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) resourceHandler.getProvider();

        ipAllocator.allocateIps(subnetResource, InternetProtocol.IPv4, input.getIps(), resource);

        deleteIdleAndConflictPorts(provider, account, subnetResource.getExternalId(), input.getIps());

        Subnet subnet = provider.buildClient(account).vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found when creating port.")
        );


        CreatePortOption option = new CreatePortOption();
        option.setName(resource.getName());
        option.setNetworkId(subnet.getNeutronNetworkId());
        option.setAdminStateUp(input.getAdminStateUp());

        List<FixedIp> fixedIps = new ArrayList<>();
        if(Utils.isEmpty(input.getIps())) {
            fixedIps.add(new FixedIp().withSubnetId(subnet.getNeutronSubnetId()));
        } else {
            fixedIps.add(new FixedIp().withIpAddress(input.getIps().get(0)).withSubnetId(subnet.getNeutronSubnetId()));
        }

        option.setFixedIps(fixedIps);

        if(Utils.isNotEmpty(securityGroups))
            option.setSecurityGroups(securityGroups.stream().map(Resource::getExternalId).toList());



        String portId = provider.buildClient(account).vpc().createPort(
                new CreatePortRequest().withBody(
                        new CreatePortRequestBody().withPort(option)
                )
        );
        resource.setExternalId(portId);
    }


    private void deleteIdleAndConflictPorts(HuaweiCloudProvider provider,
                                            ExternalAccount account,
                                            String subnetId,
                                            List<String> ips) {
        List<Port> conflictPorts = describeConflictPorts(provider, account, subnetId, ips);

        if(Utils.isEmpty(conflictPorts))
            return;

        for (Port conflictPort : conflictPorts) {
            if(Utils.isBlank(conflictPort.getDeviceId()) && filterOutRouterInterfaceAndDhcpAgent(conflictPort)){
                log.warn("Idle port with conflict ip {} detected. Deleting...",
                        JSON.toJsonString(conflictPort.getFixedIps()));
                provider.buildClient(account).vpc().deletePort(conflictPort.getId());
                log.warn("Idle port with conflict ip {} deleted.",
                        JSON.toJsonString(conflictPort.getFixedIps()));
            } else {
                log.error("Port with conflict ip %s is NOT idle, BUILD action should fail.");
            }
        }
    }

    private List<Port> describeConflictPorts(HuaweiCloudProvider provider,
                                             ExternalAccount account,
                                             String subnetId,
                                             List<String> ips){
        List<Port> result = new ArrayList<>();

        if(Utils.isBlank(subnetId) || Utils.isEmpty(ips))
            return result;

        HuaweiCloudClient client = provider.buildClient(account);

        for (String ip : ips) {
            ListPortsRequest request = new ListPortsRequest();
            request.setFixedIps(List.of("ip_address=%s".formatted(ip)));

            List<? extends Port> ports = client.vpc().describePorts(request);

            for (Port port : ports) {
                if(Utils.isEmpty(port.getFixedIps()))
                    continue;

                for (FixedIp fixedIp : port.getFixedIps()) {
                    boolean sameSubnet = Objects.equals(subnetId, fixedIp.getSubnetId());
                    boolean sameIp = Objects.equals(ip, fixedIp.getIpAddress());
                    if (sameSubnet && sameIp) {
                        result.add(port);
                        break;
                    }
                }
            }
        }

        return result;
    }



    public void deletePort(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ResourceHandler nicHandler = resource.getResourceHandler();
        HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        Optional<Port> port = client.vpc().describePort(resource.getExternalId());
        if(port.isEmpty())
            return;

        if(!resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            client.vpc().deletePort(port.get().getId());

        List<String> ips = getPortIps(port.get());

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when deleting port.")
        );

        ipAllocator.releaseIps(subnet, InternetProtocol.IPv4, ips);
    }

    private static List<String> getPortIps(Port port) {
        if(Utils.isEmpty(port.getFixedIps()))
            return List.of();

        return port.getFixedIps().stream().map(FixedIp::getIpAddress).toList();
    }


    public void ensurePortDeleted(Resource resource){
        EnsurePortDeletedWorker ensurePortDeletedWorker = new EnsurePortDeletedWorker(resource);
        SlowTaskUtil.submit(ensurePortDeletedWorker);
    }


    private record EnsurePortDeletedWorker(Resource portResource) implements Runnable {

        @Override
        public void run() {
            String portId = portResource.getExternalId();
            if (Utils.isBlank(portId))
                return;

            ResourceHandler nicHandler = portResource.getResourceHandler();
            HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();
            ExternalAccount account = provider.getAccountRepository().findExternalAccount(portResource.getAccountId());
            HuaweiCloudClient client = provider.buildClient(account);

            Optional<Port> port = client.vpc().describePort(portId);
            int count = 0;
            final int maxCount = 30;
            while (port.isPresent() && count < maxCount) {
                if (Utils.isBlank(port.get().getDeviceId())) {
                    log.warn("Remaining idle port {} of ips {} detected, deleting...",
                            portId, getPortIps(port.get()));
                    client.vpc().deletePort(portId);
                } else {
                    log.warn("Remaining port {} of ips {} is still attached to {}, checking later...({}/{})",
                            portId, getPortIps(port.get()), port.get().getDeviceOwner(), count + 1, maxCount);
                }
                SleepUtil.sleep(10);
                port = client.vpc().describePort(portId);
                count++;
            }

            if (port.isPresent())
                    log.warn("Port to be deleted still remains. Port={}.", JSON.toJsonString(port.get()));
                else
                    log.info("Remaining idle port {} has been successfully deleted.", portId);
        }
    }



    public static boolean filterOutRouterInterfaceAndDhcpAgent(Port port) {
        Port.DeviceOwnerEnum deviceOwner = port.getDeviceOwner();

        if(deviceOwner == null || Utils.isBlank(deviceOwner.getValue()))
            return true;

        if(Objects.equals(deviceOwner, Port.DeviceOwnerEnum.NETWORK_ROUTER_CENTRALIZED_SNAT)) {
            log.warn("Router interface port {} is filtered out.", port.getId());
            return false;
        }
        if(Objects.equals(deviceOwner, Port.DeviceOwnerEnum.NETWORK_ROUTER_INTERFACE_DISTRIBUTED)) {
            log.warn("Distributed router interface port {} is filtered out.", port.getId());
            return false;
        }
        if(Objects.equals(deviceOwner, Port.DeviceOwnerEnum.NETWORK_DHCP)) {
            log.warn("DHCP agent port {} is filtered out.", port.getId());
            return false;
        }

        if(deviceOwner.getValue().equalsIgnoreCase("neutron:LOADBALANCER") ||
                deviceOwner.getValue().equalsIgnoreCase("neutron:LOADBALANCERV2") ||
                deviceOwner.getValue().equalsIgnoreCase("neutron:LOADBALANCERV3")) {
            log.warn("LB port {} is filtered out.", port.getId());
            return false;
        }

        return true;
    }
}
