package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.actions;

import com.aliyun.slb20140515.models.AddBackendServersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackendId;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEniBackendHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AliyunClbEniBackendBuildHandler implements BuildResourceActionHandler {

    private final AliyunClbEniBackendHandler backendHandler;

    public AliyunClbEniBackendBuildHandler(AliyunClbEniBackendHandler backendHandler) {
        this.backendHandler = backendHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return backendHandler;
    }

    @Override
    public String getTaskName() {
        return "添加后端服务";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunClbBackendBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunClbBackendBuildInput input = JSON.convert(parameters, AliyunClbBackendBuildInput.class);

        Integer weight = input.getWeight();
        Integer port = input.getPort();

        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating backend.")
        );

        Resource nic = resource.getEssentialTarget(ResourceCategories.NIC).orElseThrow(
                () -> new StratoException("Nic not found when creating backend.")
        );

        AliyunCloudProvider provider = (AliyunCloudProvider) backendHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunNic aliyunNic = provider.buildClient(account).ecs().describeNic(nic.getExternalId()).orElseThrow(
                () -> new StratoException("External nic not found when creating backend.")
        );

        AddBackendServersRequest request = new AddBackendServersRequest();

        Map<String, Object> backendServer = new HashMap<>();

        String resourceType = "eni";

        backendServer.put("ServerId", nic.getExternalId());
        backendServer.put("Weight", weight.toString());
        backendServer.put("Type", resourceType);
        backendServer.put("ServerIp", aliyunNic.detail().getPrivateIpAddress());
        backendServer.put("Port", port.toString());
        backendServer.put("Description", resource.getDescription());

        request.setBackendServers(JSON.toJsonString(List.of(backendServer)));
        request.setLoadBalancerId(lb.getExternalId());

        provider.buildClient(account).clb().addBackendServers(request);

        AliyunClbBackendId backendId = new AliyunClbBackendId(
                lb.getExternalId(),
                resourceType,
                nic.getExternalId(),
                weight
        );

        resource.setExternalId(backendId.toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        List<Resource> targets = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER);

        if(Utils.isEmpty(targets))
            throw new BadCommandException("请指定一个CLB实例");

        if(targets.size() > 1)
            throw new BadCommandException("每个后端服务只能指定一个CLB实例");
    }
}
