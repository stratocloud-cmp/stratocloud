package com.stratocloud.provider.aliyun.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackage;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackageHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AliyunBandwidthPackageDestroyHandler implements DestroyResourceActionHandler {
    private final AliyunBandwidthPackageHandler packageHandler;

    public AliyunBandwidthPackageDestroyHandler(AliyunBandwidthPackageHandler packageHandler) {
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



        AliyunCloudProvider provider = (AliyunCloudProvider) packageHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunBandwidthPackage> bwp = packageHandler.describePackage(account, resource.getExternalId());

        if(bwp.isEmpty())
            return;

        provider.buildClient(account).vpc().deleteBandwidthPackage(resource.getExternalId());
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
