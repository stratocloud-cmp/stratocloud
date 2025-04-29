package com.stratocloud.provider.huawei.snapshot.actions;

import com.huaweicloud.sdk.evs.v2.model.SnapshotList;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.snapshot.HuaweiSnapshotHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSnapshotDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiSnapshotHandler snapshotHandler;

    public HuaweiSnapshotDestroyHandler(HuaweiSnapshotHandler snapshotHandler) {
        this.snapshotHandler = snapshotHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return snapshotHandler;
    }

    @Override
    public String getTaskName() {
        return "删除快照";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<SnapshotList> snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId());

        if(snapshot.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) snapshotHandler.getProvider();
        provider.buildClient(account).evs().deleteSnapshot(snapshot.get().getId());
    }
}
