package com.stratocloud.provider.aliyun.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.snapshot.AliyunSnapshot;
import com.stratocloud.provider.aliyun.snapshot.AliyunSnapshotHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSnapshotDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunSnapshotHandler snapshotHandler;

    public AliyunSnapshotDestroyHandler(AliyunSnapshotHandler snapshotHandler) {
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

        Optional<AliyunSnapshot> snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId());

        if(snapshot.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) snapshotHandler.getProvider();
        provider.buildClient(account).ecs().deleteSnapshot(snapshot.get().detail().getSnapshotId());
    }
}
