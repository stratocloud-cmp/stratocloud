package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.actions;

import com.aliyun.slb20140515.models.CreateMasterSlaveServerGroupRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupId;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.requirements.AliyunClbMasterSlaveGroupToMasterHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.requirements.AliyunClbMasterSlaveGroupToSlaveHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements.AliyunClbServerGroupConnectInput;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AliyunClbMasterSlaveGroupBuildHandler implements BuildResourceActionHandler {

    private final AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler;

    public AliyunClbMasterSlaveGroupBuildHandler(AliyunClbMasterSlaveGroupHandler masterSlaveGroupHandler) {
        this.masterSlaveGroupHandler = masterSlaveGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return masterSlaveGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "创建主备服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating master-slave group.")
        );

        Resource master = resource.getEssentialTargetByType(AliyunClbMasterSlaveGroupToMasterHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Master not found when creating master-slave group.")
        );

        Relationship masterRel = resource.getRelationshipByTarget(master).orElseThrow();

        AliyunClbServerGroupConnectInput masterInput
                = JSON.convert(masterRel.getProperties(), AliyunClbServerGroupConnectInput.class);

        Resource slave = resource.getEssentialTargetByType(AliyunClbMasterSlaveGroupToSlaveHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Slave not found when creating master-slave group.")
        );

        Relationship slaveRel = resource.getRelationshipByTarget(slave).orElseThrow();

        AliyunClbServerGroupConnectInput slaveInput
                = JSON.convert(slaveRel.getProperties(), AliyunClbServerGroupConnectInput.class);



        AliyunCloudProvider provider = (AliyunCloudProvider) masterSlaveGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        CreateMasterSlaveServerGroupRequest request = new CreateMasterSlaveServerGroupRequest();

        request.setLoadBalancerId(lb.getExternalId());
        request.setMasterSlaveServerGroupName(resource.getName());

        String masterSlaveBackendServers = toMasterSlaveBackendServers(master, masterInput, slave, slaveInput);

        request.setMasterSlaveBackendServers(masterSlaveBackendServers);

        AliyunClbMasterSlaveGroupId masterSlaveGroupId
                = provider.buildClient(account).clb().createMasterSlaveGroup(request);

        resource.setExternalId(masterSlaveGroupId.toString());
    }

    private static String toMasterSlaveBackendServers(Resource master,
                                                      AliyunClbServerGroupConnectInput masterInput,
                                                      Resource slave,
                                                      AliyunClbServerGroupConnectInput slaveInput) {
        Map<String, Object> masterMap = new HashMap<>();

        String resourceType = "ecs";

        masterMap.put("ServerId", master.getExternalId());
        masterMap.put("Weight", masterInput.getWeight().toString());
        masterMap.put("Type", resourceType);
        masterMap.put("Port", masterInput.getPort().toString());
        masterMap.put("ServerType", "Master");

        Map<String, Object> slaveMap = new HashMap<>();

        slaveMap.put("ServerId", slave.getExternalId());
        slaveMap.put("Weight", slaveInput.getWeight().toString());
        slaveMap.put("Type", resourceType);
        slaveMap.put("Port", slaveInput.getPort().toString());
        masterMap.put("ServerType", "Slave");

        return JSON.toJsonString(List.of(masterMap, slaveMap));
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
            throw new BadCommandException("每个虚拟服务组只能指定一个CLB实例");
    }
}
