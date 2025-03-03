package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunClbMasterSlaveGroupHandler extends AbstractResourceHandler {

    public static final String TYPE_ID = "ALIYUN_CLB_MASTER_SLAVE_GROUP";
    private final AliyunCloudProvider provider;

    public AliyunClbMasterSlaveGroupHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云主备服务器组";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_BACKEND_GROUP;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<AliyunClbMasterSlaveGroup> masterSlaveGroup = describeMasterSlaveGroup(account, externalId);
        return masterSlaveGroup.map(value -> toExternalResource(account, value));
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunClbMasterSlaveGroup serverGroup) {
        return new ExternalResource(
                getProvider().getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                serverGroup.id().toString(),
                serverGroup.detail().getMasterSlaveServerGroupName(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<AliyunClbMasterSlaveGroup> describeMasterSlaveGroup(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClbMasterSlaveGroupId masterSlaveGroupId = AliyunClbMasterSlaveGroupId.fromString(externalId);

        return provider.buildClient(account).clb().describeMasterSlaveGroup(masterSlaveGroupId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<AliyunClbMasterSlaveGroup> masterSlaveGroups = provider.buildClient(account).clb().describeMasterSlaveGroups();

        return masterSlaveGroups.stream().map(
                value -> toExternalResource(account, value)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource masterSlaveGroup = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Master-slave group not found.")
        );

        resource.updateByExternal(masterSlaveGroup);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
