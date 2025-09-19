package com.stratocloud.provider.tencent.rocketmq.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.rocketmq.RocketUtil;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.trocket.v20230308.models.DescribeInstanceResponse;
import com.tencentcloudapi.trocket.v20230308.models.ModifyInstanceRequest;
import com.tencentcloudapi.trocket.v20230308.models.ProductSKU;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentRocketResizeHandler implements ResourceActionHandler {

    private final TencentRocketHandler rocketHandler;

    public TencentRocketResizeHandler(TencentRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "变更RocketMQ实例配置";
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
        return ResizeInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<DescribeInstanceResponse> detail = client.describeRocketInstanceDetail(resource.getExternalId());

        if(detail.isEmpty())
            return Optional.empty();

        Optional<ProductSKU> sku = client.describeRocketSku(detail.get().getSkuCode());

        if(sku.isEmpty())
            return Optional.empty();

        ResizeInput input = new ResizeInput();
        input.setSkuCode(detail.get().getSkuCode());
        input.setMessageRetention(detail.get().getMessageRetention());
        input.setExtraMaxTopicNum(detail.get().getTopicNumLimit() - sku.get().getTopicNumLimit());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        List<ProductSKU> skuList = client.describeRocketSkuList().stream().sorted(
                Comparator.comparing(RocketUtil::getSkuInstanceTypePriority).thenComparing(ProductSKU::getTpsLimit)
        ).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "skuCode",
                skuList.stream().map(ProductSKU::getSkuCode).toList(),
                skuList.stream().map(RocketUtil::formatSkuName).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        ProductSKU sku = client.describeRocketSku(input.getSkuCode()).orElseThrow(
                () -> new StratoException("RocketMQ sku not found")
        );

        ModifyInstanceRequest request = new ModifyInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setSkuCode(sku.getSkuCode());

        if(Objects.equals(sku.getInstanceType(), "BASIC"))
            request.setMessageRetention(input.getMessageRetention());

        request.setMaxTopicNum(input.getExtraMaxTopicNum() + sku.getTopicNumLimit());
        request.setExtraTopicNum(input.getExtraMaxTopicNum().toString());

        client.modifyRocketInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rocket = rocketHandler.describeExternalResource(account, resource.getExternalId());

        if(rocket.isEmpty())
            return ResourceActionResult.failed("RocketMQ instance does not exist anymore");

        if(rocket.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        if(rocket.get().state() == ResourceState.ERROR)
            return ResourceActionResult.failed("RocketMQ instance is in error state");

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @SelectField(label = "集群规格")
        private String skuCode;

        @NumberField(
                label = "消息保留时长(小时)",
                min = 24,
                max = 72,
                defaultValue = 72,
                conditions = "this.skuCode && this.skuCode.indexOf('basic') !== -1"
        )
        private Long messageRetention;

        @NumberField(
                label = "额外购买Topic个数",
                defaultValue = 0
        )
        private Long extraMaxTopicNum;
    }
}
