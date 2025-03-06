package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentBandwidthPackageHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TencentBandwidthPackageDestroyHandler implements DestroyResourceActionHandler {
    private final TencentBandwidthPackageHandler packageHandler;

    public TencentBandwidthPackageDestroyHandler(TencentBandwidthPackageHandler packageHandler) {
        this.packageHandler = packageHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return packageHandler;
    }

    @Override
    public String getTaskName() {
        return "删除带宽包";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        TencentCloudProvider provider = (TencentCloudProvider) packageHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        provider.buildClient(account).deleteBandwidthPackage(resource.getExternalId());
    }


}
