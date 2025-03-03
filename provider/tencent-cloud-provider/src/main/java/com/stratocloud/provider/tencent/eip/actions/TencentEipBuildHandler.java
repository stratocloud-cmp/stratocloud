package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentEipHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.vpc.v20170312.models.AddressChargePrepaid;
import com.tencentcloudapi.vpc.v20170312.models.AllocateAddressesRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class TencentEipBuildHandler implements BuildResourceActionHandler {
    private final TencentEipHandler eipHandler;

    public TencentEipBuildHandler(TencentEipHandler eipHandler) {
        this.eipHandler = eipHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return eipHandler;
    }

    @Override
    public String getTaskName() {
        return "创建弹性IP";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentEipBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentEipBuildInput input = JSON.convert(parameters, TencentEipBuildInput.class);

        AllocateAddressesRequest request = new AllocateAddressesRequest();

        request.setInternetServiceProvider(input.getInternetServiceProvider());
        request.setInternetChargeType(input.getInternetChargeType());
        request.setInternetMaxBandwidthOut(input.getInternetMaxBandwidthOut());

        if(Objects.equals("BANDWIDTH_PREPAID_BY_MONTH", input.getInternetChargeType())){
            AddressChargePrepaid prepaid = new AddressChargePrepaid();

            prepaid.setPeriod(Long.valueOf(input.getPrepaidPeriod()));
            prepaid.setAutoRenewFlag(Long.valueOf(input.getRenewFlag()));

            request.setAddressChargePrepaid(prepaid);
        }else if(Objects.equals("BANDWIDTH_PACKAGE", input.getInternetChargeType())){
            Resource bwp = resource.getExclusiveTarget(ResourceCategories.BANDWIDTH_PACKAGE).orElseThrow(
                    () -> new StratoException("No bandwidth package found.")
            );

            request.setBandwidthPackageId(bwp.getExternalId());
        }

        request.setAddressName(resource.getName());

        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        String eipId = provider.buildClient(account).createEip(request);

        resource.setExternalId(eipId);
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of(new ResourceUsage(UsageTypes.ELASTIC_IP.type(), BigDecimal.ONE));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
