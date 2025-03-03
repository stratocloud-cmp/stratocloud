package com.stratocloud.provider.aliyun.eip.actions;

import com.aliyun.vpc20160428.models.CreateCommonBandwidthPackageRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackageHandler;
import com.stratocloud.provider.aliyun.eip.AliyunBwpChargeType;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AliyunBandwidthPackageBuildHandler implements BuildResourceActionHandler {
    private final AliyunBandwidthPackageHandler packageHandler;

    public AliyunBandwidthPackageBuildHandler(AliyunBandwidthPackageHandler packageHandler) {
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
        return AliyunBandwidthPackageBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBandwidthPackageBuildInput input = JSON.convert(parameters, AliyunBandwidthPackageBuildInput.class);

        Optional<Resource> zone = resource.getEssentialTarget(ResourceCategories.ZONE);

        CreateCommonBandwidthPackageRequest request = new CreateCommonBandwidthPackageRequest();
        request.setBandwidth(input.getBandwidth());
        request.setDescription(resource.getDescription());
        request.setISP(input.getIsp());
        request.setInternetChargeType(input.getChargeType().getId());
        request.setName(resource.getName());

        if(input.getRatio() != null && input.getChargeType() == AliyunBwpChargeType.PayBy95)
            request.setRatio(input.getRatio());

        zone.ifPresent(value -> request.setZone(value.getExternalId()));

        AliyunCloudProvider provider = (AliyunCloudProvider) packageHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String packageId = provider.buildClient(account).vpc().createBandwidthPackage(request);
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
