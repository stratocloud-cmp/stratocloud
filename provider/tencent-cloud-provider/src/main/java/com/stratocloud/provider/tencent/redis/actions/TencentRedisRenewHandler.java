package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.RedisUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.redis.v20180412.models.InquiryPriceCreateInstanceRequest;
import com.tencentcloudapi.redis.v20180412.models.InquiryPriceCreateInstanceResponse;
import com.tencentcloudapi.redis.v20180412.models.RenewInstanceRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentRedisRenewHandler implements ResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisRenewHandler(TencentRedisHandler redisHandler) {
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
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var instance = redisHandler.describeRedis(account, resource.getExternalId());

        if(instance.isEmpty())
            return Optional.empty();

        RenewInput input = new RenewInput();
        input.setPrepaid(RedisUtil.isPrepaid(instance.get()));
        input.setModifyPayMode(true);
        input.setPeriod(1L);

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(RenewInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        RenewInstanceRequest request = new RenewInstanceRequest();

        request.setInstanceId(resource.getExternalId());
        request.setPeriod(input.getPeriod());
        request.setModifyPayMode(input.isPrepaid() ? null : "prepaid");

        client.renewRedisInstance(request);
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

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        if(instance.isPresent() && !RedisUtil.isPrepaid(instance.get()) && !input.isModifyPayMode())
            throw new BadCommandException("按量计费实例必须勾选 <转为包年包月实例>");
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        var instance = redisHandler.describeRedis(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceCost.ZERO;

        RenewInput input = JSON.convert(parameters, RenewInput.class);

        InquiryPriceCreateInstanceRequest request = new InquiryPriceCreateInstanceRequest();
        request.setTypeId(instance.get().getType());
        request.setMemSize(instance.get().getRedisShardSize());
        request.setGoodsNum(1L);
        request.setPeriod(input.getPeriod());
        request.setBillingMode(1L);
        request.setZoneId(instance.get().getZoneId());
        request.setRedisShardNum(instance.get().getRedisShardNum());
        request.setRedisReplicasNum(instance.get().getRedisReplicasNum());
        request.setProductVersion(instance.get().getProductVersion());

        InquiryPriceCreateInstanceResponse response = client.describeRedisPrice(request);
        Float price = response.getPrice();
        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price/100.0, input.getPeriod(), ChronoUnit.MONTHS);
    }

    @Data
    public static class RenewInput implements ResourceActionInput {
        @BooleanField(label = "是否为预付费实例", conditions = "false")
        private boolean prepaid;

        @BooleanField(label = "转为包年包月实例", defaultValue = true, conditions = "this.prepaid === false")
        private boolean modifyPayMode;

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
    }
}
