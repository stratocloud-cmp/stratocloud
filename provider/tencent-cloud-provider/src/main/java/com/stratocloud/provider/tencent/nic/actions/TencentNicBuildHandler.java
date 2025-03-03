package com.stratocloud.provider.tencent.nic.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.nic.NicQosLevel;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class TencentNicBuildHandler implements BuildResourceActionHandler {
    private final TencentNicHandler nicHandler;
    private final IpAllocator ipAllocator;

    public TencentNicBuildHandler(TencentNicHandler nicHandler,
                                  IpAllocator ipAllocator) {
        this.nicHandler = nicHandler;
        this.ipAllocator = ipAllocator;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nicHandler;
    }

    @Override
    public String getTaskName() {
        return "创建弹性网卡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentNicBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentNicBuildInput input = JSON.convert(parameters, TencentNicBuildInput.class);

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating nic.")
        );
        Resource vpc = subnet.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating nic.")
        );

        ipAllocator.allocateIps(subnet, InternetProtocol.IPv4, input.getIps(), resource);

        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            configurePrimaryNic(resource, input);
        else
            createSecondaryNic(resource, input, subnet, vpc);
    }

    private void configurePrimaryNic(Resource resource, TencentNicBuildInput input) {
        log.info("Configuring Tencent primary nic: {}", resource.getName());
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) nicHandler.getProvider();

        Optional<NetworkInterface> networkInterface = nicHandler.describeNic(account, resource.getExternalId());

        if(networkInterface.isEmpty()) {
            log.warn("Tencent primary nic {} not created yet, something went wrong.", resource.getName());
            return;
        }

        TencentCloudClient client = provider.buildClient(account);

        ModifyNetworkInterfaceAttributeRequest modifyRequest = new ModifyNetworkInterfaceAttributeRequest();
        modifyRequest.setNetworkInterfaceId(networkInterface.get().getNetworkInterfaceId());

        modifyRequest.setNetworkInterfaceName(resource.getName());
        modifyRequest.setNetworkInterfaceDescription(resource.getDescription());

        if(input.getTrunkingFlag() != null)
            modifyRequest.setTrunkingFlag(input.getTrunkingFlag().name());

        log.info("Modifying primary nic attributes...");
        client.modifyNic(modifyRequest);

        if(input.getQosLevel() != null){
            ModifyNetworkInterfaceQosRequest modifyQosRequest = new ModifyNetworkInterfaceQosRequest();
            modifyQosRequest.setNetworkInterfaceIds(new String[]{networkInterface.get().getNetworkInterfaceId()});
            modifyQosRequest.setQosLevel(input.getQosLevel().name());

            log.info("Modifying primary nic qos level...");
            client.modifyNicQosLevel(modifyQosRequest);
        }

        Integer newIpCount = input.getSecondaryPrivateIpAddressCount();
        if((newIpCount != null && newIpCount > 0) || Utils.isNotEmpty(input.getIps())){
            AssignPrivateIpAddressesRequest assignRequest = new AssignPrivateIpAddressesRequest();
            assignRequest.setNetworkInterfaceId(networkInterface.get().getNetworkInterfaceId());

            if(Utils.isNotEmpty(input.getIps())){
                List<String> ips = new ArrayList<>(input.getIps());

                if(Utils.isNotEmpty(networkInterface.get().getPrivateIpAddressSet())){
                    for (var spec : networkInterface.get().getPrivateIpAddressSet()) {
                        ips.remove(spec.getPrivateIpAddress());
                    }
                }

                assignRequest.setPrivateIpAddresses(
                        toPrivateIpSpecs(ips, input.getQosLevel(), false)
                );
            }


            if(newIpCount != null && newIpCount > 0)
                assignRequest.setSecondaryPrivateIpAddressCount(Long.valueOf(newIpCount));

            if(input.getQosLevel() != null)
                assignRequest.setQosLevel(input.getQosLevel().name());

            log.info("Assigning primary nic private ips...");
            client.assignPrivateIps(assignRequest);
        }

        log.info("Tencent primary nic {} configured successfully.", resource.getName());
    }

    private void createSecondaryNic(Resource resource, TencentNicBuildInput input, Resource subnet, Resource vpc) {
        log.info("Creating Tencent secondary nic: {}.", resource.getName());

        CreateNetworkInterfaceRequest request = new CreateNetworkInterfaceRequest();

        request.setVpcId(vpc.getExternalId());
        request.setSubnetId(subnet.getExternalId());

        request.setNetworkInterfaceName(resource.getName());

        request.setNetworkInterfaceDescription(resource.getDescription());

        if(input.getSecondaryPrivateIpAddressCount()!=null){
            request.setSecondaryPrivateIpAddressCount(Long.valueOf(input.getSecondaryPrivateIpAddressCount()));
        }

        if(input.getQosLevel() != null)
            request.setQosLevel(input.getQosLevel().name());

        if(input.getTrunkingFlag() != null)
            request.setTrunkingFlag(input.getTrunkingFlag().name());

        if(Utils.isNotEmpty(input.getIps()))
            request.setPrivateIpAddresses(
                    toPrivateIpSpecs(
                            input.getIps(),
                            input.getQosLevel(),
                            true
                    )
            );


        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) nicHandler.getProvider();

        NetworkInterface nic = provider.buildClient(account).createNic(request);
        resource.setExternalId(nic.getNetworkInterfaceId());

        log.info("Tencent secondary nic created successfully. External ID: {}.", resource.getExternalId());
    }

    private PrivateIpAddressSpecification[] toPrivateIpSpecs(List<String> ips,
                                                             NicQosLevel qosLevel,
                                                             boolean setFirstIpPrimary) {
        if(Utils.isEmpty(ips))
            return new PrivateIpAddressSpecification[0];

        PrivateIpAddressSpecification[] specs = new PrivateIpAddressSpecification[ips.size()];

        for (int i=0;i<ips.size();i++) {
            PrivateIpAddressSpecification spec = new PrivateIpAddressSpecification();
            spec.setPrivateIpAddress(ips.get(i));
            spec.setPrimary(setFirstIpPrimary && i==0);

            if(qosLevel != null)
                spec.setQosLevel(qosLevel.name());

            specs[i] = spec;
        }

        return specs;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        TencentNicBuildInput input = JSON.convert(parameters, TencentNicBuildInput.class);

        int ipCount;

        if(input.getSecondaryPrivateIpAddressCount() != null)
            ipCount = input.getSecondaryPrivateIpAddressCount()+Utils.length(input.getIps());
        else
            ipCount = Utils.length(input.getIps());

        return List.of(new ResourceUsage(
                UsageTypes.NIC_IP.type(), BigDecimal.valueOf(ipCount)
        ));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
