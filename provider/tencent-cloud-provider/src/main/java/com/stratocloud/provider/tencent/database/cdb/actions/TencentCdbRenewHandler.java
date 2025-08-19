package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import com.tencentcloudapi.cdb.v20170320.models.RenewDBInstanceRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCdbRenewHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbRenewHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
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
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceInfo> cdb = cdbHandler.describeCdb(account, resource.getExternalId());

        if(cdb.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(RenewInput.class);

        RenewInput renewInput = new RenewInput();
        renewInput.setPayType(String.valueOf(cdb.get().getPayType()));
        renewInput.setModifyPayType(true);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, renewInput);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RenewInput input = JSON.convert(parameters, RenewInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        RenewDBInstanceRequest request = new RenewDBInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setTimeSpan(input.getTimeSpan());
        request.setModifyPayType(input.isModifyPayType() ? "PREPAID" : null);
        request.setAutoRenew(input.isAutoRenew() ? 1L : 0L);

        client.renewCdbInstance(request);
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
        RenewInput input = JSON.convert(parameters, RenewInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        InstanceInfo instanceInfo = cdbHandler.describeCdb(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("云数据库不存在")
        );
        if(!input.isModifyPayType() && Objects.equals(instanceInfo.getPayType(), 1L))
            throw new BadCommandException("未允许将按量计费实例转换为包年包月实例");
    }

    @Data
    public static class RenewInput implements ResourceActionInput {
        @InputField(label = "当前计费方式", disabled = true, conditions = "false")
        private String payType;
        @BooleanField(label = "允许转换为包年包月实例", defaultValue = true, conditions = "this.payType==='1'")
        private boolean modifyPayType;
        @SelectField(
                label = "续费时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                        "1年", "2年", "3年"
                },
                defaultValues = "1"
        )
        private Long timeSpan;
        @BooleanField(label = "自动续费")
        private boolean autoRenew;
    }
}
