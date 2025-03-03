package com.stratocloud.provider.aliyun.eip.actions;

import com.aliyun.vpc20160428.models.AllocateEipAddressRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunEipHandler;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class AliyunEipBuildHandler implements BuildResourceActionHandler {
    private final AliyunEipHandler eipHandler;

    public AliyunEipBuildHandler(AliyunEipHandler eipHandler) {
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
        return AliyunEipBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunEipBuildInput input = JSON.convert(parameters, AliyunEipBuildInput.class);

        AllocateEipAddressRequest request = new AllocateEipAddressRequest();
        request.setAutoPay(true);
        request.setBandwidth(String.valueOf(input.getBandwidth()));
        request.setISP(input.getIsp());
        request.setInstanceChargeType(input.getInstanceChargeType());


        if(Objects.equals("PrePaid", input.getInstanceChargeType())){
            request.setInternetChargeType("PayByBandwidth");

            if(input.getPrepaidPeriod() <= 9){
                request.setPeriod(input.getPrepaidPeriod());
                request.setPricingCycle("Month");
            }else {
                request.setPeriod(input.getPrepaidPeriod()/12);
                request.setPricingCycle("Year");
            }
        }else {
            request.setInternetChargeType(input.getInternetChargeType());
        }


        if(Utils.isNotEmpty(input.getSecurityProtectionTypes()))
            request.setSecurityProtectionTypes(input.getSecurityProtectionTypes());

        request.setName(resource.getName());
        request.setDescription(resource.getDescription());


        request.setISP(input.getIsp());

        request.setBandwidth(String.valueOf(input.getBandwidth()));


        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        String eipId = provider.buildClient(account).vpc().createEip(request);

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
