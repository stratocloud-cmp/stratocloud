package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentCdbDestroyHandler implements DestroyResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbDestroyHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁云数据库";
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

        Optional<ExternalResource> cdb = cdbHandler.describeExternalResource(account, resource.getExternalId());

        if(cdb.isEmpty())
            return;

        client.offlineCdbInstance(cdb.get().externalId());
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> cdb = cdbHandler.describeExternalResource(account, resource.getExternalId());

        if(cdb.isPresent() && cdb.get().state() != ResourceState.SHUTDOWN)
            throw new BadCommandException("请先隔离数据库");
    }
}
