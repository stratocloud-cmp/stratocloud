package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.DescribeRenewalPriceRequest;
import com.aliyun.rds20140815.models.RenewInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunRdsRenewHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsRenewHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RENEW;
    }

    @Override
    public String getTaskName() {
        return "续费云数据库";
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
        RenewInput input = JSON.convert(parameters, RenewInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        var client = provider.buildClient(account);

        RenewInstanceRequest request = new RenewInstanceRequest();
        request.setDBInstanceId(resource.getExternalId());
        request.setPeriod(input.getPeriod().intValue());
        request.setAutoRenew(input.getAutoRenew());
        request.setAutoPay("True");
        request.setAutoUseCoupon(input.isAutoUseCoupon());

        client.rds().renewInstance(request);
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
        var instanceInfo = rdsHandler.describeRds(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("云数据库不存在")
        );
        if(!Objects.equals(instanceInfo.detail().getPayType(), "Prepaid"))
            throw new BadCommandException("只有包年包月实例可以续费");
    }

    @Data
    public static class RenewInput implements ResourceActionInput {
        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36", "48", "60"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月",
                        "1年", "2年", "3年", "4年", "5年"
                },
                defaultValues = "1"
        )
        private Long period;

        @SelectField(
                label = "自动续费",
                options = {
                        "false",
                        "true"
                },
                optionNames = {
                        "否",
                        "是"
                },
                defaultValues = "false"
        )
        private String autoRenew;

        @BooleanField(label = "自动使用代金券")
        private boolean autoUseCoupon;
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var instanceInfo = rdsHandler.describeRds(account, resource.getExternalId());

        if(instanceInfo.isEmpty())
            return ResourceCost.ZERO;

        RenewInput input = JSON.convert(parameters, RenewInput.class);
        var rds = instanceInfo.get();

        DescribeRenewalPriceRequest request = new DescribeRenewalPriceRequest();

        Long period = input.getPeriod();
        if(period == null)
            return ResourceCost.ZERO;

        double timeAmount = period;
        ChronoUnit timeUnit = ChronoUnit.MONTHS;

        request.setPayType("Prepaid");
        request.setDBInstanceId(rds.detail().getDBInstanceId());
        request.setUsedTime(period >= 12 ? period.intValue() /12 : period.intValue());
        request.setTimeType(period >= 12 ? "Year" : "Month");

        var response = client.rds().describeRenewalPrice(request);

        var price = response.getPriceInfo();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price.getTradePrice(), timeAmount, timeUnit);
    }
}
