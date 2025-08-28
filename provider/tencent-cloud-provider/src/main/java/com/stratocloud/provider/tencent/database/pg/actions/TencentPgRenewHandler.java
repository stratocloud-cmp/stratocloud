package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import com.tencentcloudapi.postgres.v20170312.models.InquiryPriceRenewDBInstanceRequest;
import com.tencentcloudapi.postgres.v20170312.models.InquiryPriceRenewDBInstanceResponse;
import com.tencentcloudapi.postgres.v20170312.models.RenewInstanceRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentPgRenewHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgRenewHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RENEW;
    }

    @Override
    public String getTaskName() {
        return "续费PostgreSQL实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RenewInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        RenewInstanceRequest request = new RenewInstanceRequest();

        request.setDBInstanceId(resource.getExternalId());
        request.setPeriod(input.getPeriod());
        request.setAutoVoucher(input.getAutoVoucher());

        client.renewPgInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<DBInstance> instance = pgHandler.describePg(account, resource.getExternalId());

        if(instance.isPresent() && !PgUtil.isPrepaid(instance.get()))
            throw new BadCommandException("按量计费实例无法续费");
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        InquiryPriceRenewDBInstanceRequest request = new InquiryPriceRenewDBInstanceRequest();
        request.setDBInstanceId(resource.getExternalId());
        request.setPeriod(input.getPeriod());

        InquiryPriceRenewDBInstanceResponse response = provider.buildClient(account).describePgRenewPrice(request);
        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price / 100.0, input.getPeriod(), ChronoUnit.MONTHS);
    }

    @Data
    public static class RenewInput implements ResourceActionInput {
        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                        "1年", "2年", "3年"
                },
                defaultValues = "1"
        )
        private Long period;
        @SelectField(
                label = "是否自动使用代金券",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "否",
                        "是"
                },
                defaultValues = "0"
        )
        private Long autoVoucher;
    }
}
