package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCdbRemoveIsolationHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbRemoveIsolationHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.REMOVE_ISOLATION;
    }

    @Override
    public String getTaskName() {
        return "解隔离云数据库";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.SHUTDOWN);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        client.releaseIsolatedHourInstance(resource.getExternalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        InstanceInfo instanceInfo = cdbHandler.describeCdb(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("云数据库不存在")
        );
        if(Objects.equals(instanceInfo.getPayType(), 0L))
            throw new BadCommandException("包年包月实例请选择续费操作");
    }
}
