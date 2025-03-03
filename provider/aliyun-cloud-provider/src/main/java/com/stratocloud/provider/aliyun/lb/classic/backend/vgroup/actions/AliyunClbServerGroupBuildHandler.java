package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.actions;

import com.aliyun.slb20140515.models.CreateVServerGroupRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupId;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunClbServerGroupBuildHandler implements BuildResourceActionHandler {

    private final AliyunClbServerGroupHandler serverGroupHandler;

    public AliyunClbServerGroupBuildHandler(AliyunClbServerGroupHandler serverGroupHandler) {
        this.serverGroupHandler = serverGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "创建服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating backend.")
        );

        AliyunCloudProvider provider = (AliyunCloudProvider) serverGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        CreateVServerGroupRequest request = new CreateVServerGroupRequest();

        request.setLoadBalancerId(lb.getExternalId());
        request.setVServerGroupName(resource.getName());

        AliyunClbServerGroupId serverGroupId = provider.buildClient(account).clb().createServerGroup(request);

        resource.setExternalId(serverGroupId.toString());
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
