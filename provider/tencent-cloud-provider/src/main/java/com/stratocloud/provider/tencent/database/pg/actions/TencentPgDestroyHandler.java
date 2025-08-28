package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentPgDestroyHandler implements DestroyResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgDestroyHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁PostgreSQL实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<DBInstance> instance = pgHandler.describePg(account, resource.getExternalId());
        if(instance.isEmpty())
            return;

        client.destroyPgInstance(resource.getExternalId());
    }
}
