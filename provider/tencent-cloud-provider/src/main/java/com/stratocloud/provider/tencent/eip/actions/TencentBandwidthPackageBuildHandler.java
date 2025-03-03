package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentBandwidthPackageHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.vpc.v20170312.models.CreateBandwidthPackageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TencentBandwidthPackageBuildHandler implements BuildResourceActionHandler {
    private final TencentBandwidthPackageHandler packageHandler;

    public TencentBandwidthPackageBuildHandler(TencentBandwidthPackageHandler packageHandler) {
        this.packageHandler = packageHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return packageHandler;
    }

    @Override
    public String getTaskName() {
        return "创建带宽包";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBandwidthPackageBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBandwidthPackageBuildInput input = JSON.convert(parameters, TencentBandwidthPackageBuildInput.class);

        CreateBandwidthPackageRequest request = new CreateBandwidthPackageRequest();
        request.setNetworkType(input.getNetworkType());
        request.setChargeType(input.getChargeType());
        request.setBandwidthPackageName(resource.getName());
        request.setInternetMaxBandwidth(input.getInternetMaxBandwidth());
        request.setProtocol(input.getProtocol());
        request.setTimeSpan(input.getTimeSpan());

        TencentCloudProvider provider = (TencentCloudProvider) packageHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String packageId = provider.buildClient(account).createBandwidthPackage(request);
        resource.setExternalId(packageId);
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
