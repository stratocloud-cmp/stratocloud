package com.stratocloud.provider.aliyun.lb.classic.listener.udp.actions;

import com.aliyun.slb20140515.models.CreateLoadBalancerUDPListenerRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToAclInput;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements.AliyunUdpListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements.AliyunUdpListenerToIntranetClbHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunUdpListenerBuildHandler implements BuildResourceActionHandler {

    private final AliyunUdpListenerHandler listenerHandler;

    public AliyunUdpListenerBuildHandler(AliyunUdpListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunUdpListenerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, AliyunUdpListenerBuildInput.class);

        CreateLoadBalancerUDPListenerRequest request = new CreateLoadBalancerUDPListenerRequest();

        resolveBasicOptions(resource, input, request);

        resolveHealthCheckOptions(input, request);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();

        provider.buildClient(account).clb().createUdpListener(request);
    }

    private static void resolveBasicOptions(Resource resource,
                                            AliyunUdpListenerBuildInput input,
                                            CreateLoadBalancerUDPListenerRequest request) {
        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating listener.")
        );

        Optional<Resource> backendGroup = resource.getExclusiveTarget(
                ResourceCategories.LOAD_BALANCER_BACKEND_GROUP
        );

        Optional<Relationship> aclRequirement = resource.getExclusiveRequirements().stream().filter(
                rel -> rel.getTarget().isCategory(ResourceCategories.LOAD_BALANCER_ACL)
        ).findAny();

        request.setLoadBalancerId(lb.getExternalId());

        if(backendGroup.isPresent()){
            String groupResourceType = backendGroup.get().getType();

            if(Objects.equals(AliyunClbServerGroupHandler.TYPE_ID, groupResourceType))
                request.setVServerGroupId(backendGroup.get().getExternalId());
            else if(Objects.equals(AliyunClbMasterSlaveGroupHandler.TYPE_ID, groupResourceType))
                request.setMasterSlaveServerGroupId(backendGroup.get().getExternalId());
        }

        if (aclRequirement.isPresent()) {
            var aclInput = JSON.convert(aclRequirement.get().getProperties(), AliyunListenerToAclInput.class);

            Resource acl = aclRequirement.get().getTarget();

            request.setAclId(acl.getExternalId());
            request.setAclStatus("on");

            if(Utils.isNotBlank(aclInput.getAclType()))
                request.setAclType(aclInput.getAclType());
        }

        request.setListenerPort(input.getPort());
        request.setBackendServerPort(input.getBackendPort());
        request.setBandwidth(input.getBandwidth());
        request.setScheduler(input.getScheduler());

        request.setDescription(resource.getDescription());

        AliyunListenerId listenerId = new AliyunListenerId(
                lb.getExternalId(),
                "udp",
                input.getPort().toString()
        );
        resource.setExternalId(listenerId.toString());
    }

    private static void resolveHealthCheckOptions(AliyunUdpListenerBuildInput input,
                                                  CreateLoadBalancerUDPListenerRequest request) {
        boolean enableHealthCheck = input.getEnableHealthCheck()!=null ? input.getEnableHealthCheck() : false;
        if(enableHealthCheck){
            request.setHealthCheckSwitch("on");

            request.setHealthCheckConnectPort(input.getHealthCheckConnectPort());

            request.setHealthCheckInterval(input.getHealthCheckInterval());
            request.setHealthCheckConnectTimeout(input.getHealthCheckTimeout());

            request.setHealthyThreshold(input.getHealthyThreshold());
            request.setUnhealthyThreshold(input.getUnhealthyThreshold());

            request.setHealthCheckReq(input.getHealthCheckReq());
            request.setHealthCheckExp(input.getHealthCheckExp());
        }else {
            request.setHealthCheckSwitch("off");
        }
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        List<Resource> targets = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER);

        if(Utils.isEmpty(targets))
            throw new BadCommandException("请指定一个LB实例");

        if(targets.size() > 1)
            throw new BadCommandException("每个监听器只能指定一个LB实例");

        List<Resource> backendGroups
                = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER_BACKEND_GROUP);

        if(Utils.length(backendGroups) > 1)
            throw new BadCommandException("每个监听器只能指定一个LB实例");
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunUdpListenerToInternetClbHandler.TYPE_ID,
                AliyunUdpListenerToIntranetClbHandler.TYPE_ID
        );
    }
}
