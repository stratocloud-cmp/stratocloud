package com.stratocloud.provider.aliyun.nic.actions;

import com.aliyun.ecs20140526.models.AssignPrivateIpAddressesRequest;
import com.aliyun.ecs20140526.models.CreateNetworkInterfaceRequest;
import com.aliyun.ecs20140526.models.ModifyNetworkInterfaceAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AliyunNicBuildHandler implements BuildResourceActionHandler {
    private final AliyunNicHandler nicHandler;
    private final IpAllocator ipAllocator;

    public AliyunNicBuildHandler(AliyunNicHandler nicHandler,
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
        return AliyunNicBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunNicBuildInput input = JSON.convert(parameters, AliyunNicBuildInput.class);

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating nic.")
        );

        ipAllocator.allocateIps(subnet, InternetProtocol.IPv4, input.getIps(), resource);

        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            configurePrimaryNic(resource, input);
        else
            createSecondaryNic(resource, input, subnet);
    }

    private void configurePrimaryNic(Resource resource, AliyunNicBuildInput input) {
        log.info("Configuring Aliyun primary nic: {}", resource.getName());

        List<Resource> securityGroups = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        Optional<AliyunNic> nic = nicHandler.describeNic(account, resource.getExternalId());

        if(nic.isEmpty()) {
            log.warn("Aliyun primary nic {} not created yet, something went wrong.", resource.getName());
            return;
        }

        AliyunClient client = provider.buildClient(account);

        ModifyNetworkInterfaceAttributeRequest modifyRequest = new ModifyNetworkInterfaceAttributeRequest();
        modifyRequest.setNetworkInterfaceId(nic.get().detail().getNetworkInterfaceId());

        modifyRequest.setNetworkInterfaceName(resource.getName());
        modifyRequest.setDescription(resource.getDescription());

        modifyRequest.setSecurityGroupId(securityGroups.stream().map(Resource::getExternalId).toList());

        if(input.getTrafficMode() != null){
            var trafficConfig = new ModifyNetworkInterfaceAttributeRequest.ModifyNetworkInterfaceAttributeRequestNetworkInterfaceTrafficConfig();
            trafficConfig.setNetworkInterfaceTrafficMode(input.getTrafficMode().name());
        }

        log.info("Modifying primary nic attributes...");
        client.ecs().modifyNic(modifyRequest);


        Integer newIpCount = input.getSecondaryPrivateIpAddressCount();
        if((newIpCount != null && newIpCount > 0) || Utils.isNotEmpty(input.getIps())){
            AssignPrivateIpAddressesRequest assignRequest = new AssignPrivateIpAddressesRequest();
            assignRequest.setNetworkInterfaceId(nic.get().detail().getNetworkInterfaceId());

            if(Utils.isNotEmpty(input.getIps())){
                List<String> ips = new ArrayList<>(input.getIps());
                ips.removeAll(nic.get().getPrivateIps());
                assignRequest.setPrivateIpAddress(ips);
            }


            if(newIpCount != null && newIpCount > 0)
                assignRequest.setSecondaryPrivateIpAddressCount(newIpCount);

            log.info("Assigning primary nic private ips...");
            client.ecs().assignPrivateIps(assignRequest);
        }

        log.info("Aliyun primary nic {} configured successfully.", resource.getName());
    }

    private void createSecondaryNic(Resource resource,
                                    AliyunNicBuildInput input,
                                    Resource subnet) {
        log.info("Creating Aliyun secondary nic: {}.", resource.getName());

        List<Resource> securityGroups = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        CreateNetworkInterfaceRequest request = new CreateNetworkInterfaceRequest();

        request.setVSwitchId(subnet.getExternalId());

        request.setNetworkInterfaceName(resource.getName());

        request.setDescription(resource.getDescription());

        request.setSecurityGroupIds(securityGroups.stream().map(Resource::getExternalId).toList());

        if(input.getSecondaryPrivateIpAddressCount()!=null && input.getSecondaryPrivateIpAddressCount()>0){
            request.setSecondaryPrivateIpAddressCount(input.getSecondaryPrivateIpAddressCount());
        }

        if(Utils.isNotEmpty(input.getIps())) {
            request.setPrimaryIpAddress(input.getIps().get(0));

            if(input.getIps().size() > 1)
                request.setPrivateIpAddress(input.getIps().subList(1, input.getIps().size()));
        }


        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        String nicId = provider.buildClient(account).ecs().createNic(request);
        resource.setExternalId(nicId);

        log.info("Aliyun secondary nic created successfully. External ID: {}.", resource.getExternalId());
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        AliyunNicBuildInput input = JSON.convert(parameters, AliyunNicBuildInput.class);

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
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            return;

        List<Resource> securityGroups = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        if(Utils.isEmpty(securityGroups))
            throw new BadCommandException("请为辅助网卡至少指定一个安全组");


    }
}
