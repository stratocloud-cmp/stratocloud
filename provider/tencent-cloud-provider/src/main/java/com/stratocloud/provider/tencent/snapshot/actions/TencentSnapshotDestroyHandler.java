package com.stratocloud.provider.tencent.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.snapshot.TencentSnapshotHandler;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.cbs.v20170312.models.Snapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentSnapshotDestroyHandler implements DestroyResourceActionHandler {

    private final TencentSnapshotHandler snapshotHandler;

    public TencentSnapshotDestroyHandler(TencentSnapshotHandler snapshotHandler) {
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

        Optional<Snapshot> snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId());

        if(snapshot.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) snapshotHandler.getProvider();
        provider.buildClient(account).deleteSnapshot(snapshot.get().getSnapshotId());
    }
}
