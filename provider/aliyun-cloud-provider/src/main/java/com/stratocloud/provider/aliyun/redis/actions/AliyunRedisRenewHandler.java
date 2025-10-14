package com.stratocloud.provider.aliyun.redis.actions;

import com.aliyun.r_kvstore20150101.models.DescribePriceRequest;
import com.aliyun.r_kvstore20150101.models.DescribePriceResponseBody;
import com.aliyun.r_kvstore20150101.models.RenewInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
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
public class AliyunRedisRenewHandler implements ResourceActionHandler {

    private final AliyunRedisHandler redisHandler;

    public AliyunRedisRenewHandler(AliyunRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RENEW;
    }

    @Override
    public String getTaskName() {
        return "续费Redis实例";
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
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        RenewInstanceRequest request = new RenewInstanceRequest();

        request.setInstanceId(resource.getExternalId());
        request.setPeriod(input.getPeriod());

        client.tair().renewInstance(request);
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

        var instance = redisHandler.describeRedis(account, resource.getExternalId());

        if(instance.isPresent() && Objects.equals(instance.get().detail().getChargeType(), "PostPaid"))
            throw new BadCommandException("按量计费实例无法进行续费操作");
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) redisHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var attributes = client.tair().describeInstanceAttributes(resource.getExternalId());

        if(attributes.isEmpty())
            return ResourceCost.ZERO;

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        DescribePriceRequest request = new DescribePriceRequest();

        request.setInstanceId(resource.getExternalId());
        request.setOrderType("RENEW");
        request.setPeriod(input.getPeriod());

        DescribePriceResponseBody responseBody = client.tair().describePrice(request);

        if(responseBody.getOrder() == null)
            return ResourceCost.ZERO;

        if(Objects.equals(attributes.get().detail().getChargeType(), "PrePaid")){
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    attributes.get().getMonthPeriod(),
                    ChronoUnit.MONTHS
            );
        }else {
            return new ResourceCost(
                    Double.parseDouble(responseBody.getOrder().getTradeAmount()),
                    1.0,
                    ChronoUnit.HOURS
            );
        }
    }

    @Data
    public static class RenewInput implements ResourceActionInput {
        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月",
                        "1年", "2年", "3年"
                },
                defaultValues = "1"
        )
        private Long period;
    }
}
