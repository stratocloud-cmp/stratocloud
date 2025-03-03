package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.InstanceRefund;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentInstanceDestroyHandler implements DestroyResourceActionHandler {
    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceDestroyHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        Optional<Instance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        if(Objects.equals(instance.get().getInstanceState(), "LAUNCH_FAILED"))
            return;


        TencentCloudClient client = provider.buildClient(account);
        client.terminateInstance(instance.get().getInstanceId());
    }


    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        Optional<Instance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceCost.ZERO;

        InstanceRefund refund = provider.buildClient(account).inquiryPriceTerminateInstance(
                instance.get().getInstanceId()
        );

        String chargeType = instance.get().getInstanceChargeType();

        if("PREPAID".equals(chargeType)) {
            LocalDateTime expiredTime = TencentTimeUtil.toLocalDateTime(instance.get().getExpiredTime());
            long months = ChronoUnit.MONTHS.between(LocalDateTime.now(), expiredTime);
            return new ResourceCost(-refund.getRefunds(), months, ChronoUnit.MONTHS);
        }else {
            return new ResourceCost(
                    refund.getRefunds() != null ? refund.getRefunds() : 0.0,
                    0,
                    ChronoUnit.HOURS
            );
        }
    }
}
