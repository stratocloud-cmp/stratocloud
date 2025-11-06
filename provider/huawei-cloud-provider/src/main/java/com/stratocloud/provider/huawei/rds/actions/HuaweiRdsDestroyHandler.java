package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.ChargeInfoResponse;
import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiRdsDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsDestroyHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁RDS实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceResponse> instance = rdsHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        ChargeInfoResponse chargeInfo = instance.get().getChargeInfo();

        if(chargeInfo == null || Objects.equals(chargeInfo.getChargeMode(), ChargeInfoResponse.ChargeModeEnum.POSTPAID))
            provider.buildClient(account).rds().deleteInstance(instance.get().getId());
        else
            provider.buildClient(account).bss().unsubscribeResource(instance.get().getId());
    }
}
