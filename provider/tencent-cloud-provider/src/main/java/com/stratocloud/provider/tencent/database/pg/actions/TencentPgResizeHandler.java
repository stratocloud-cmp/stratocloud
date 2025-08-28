package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DateTimeField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.util.PgInstanceClass;
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentPgResizeHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgResizeHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "云数据库调整配置";
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();

        Optional<DBInstance> pg = pgHandler.describePg(account, resource.getExternalId());

        if(pg.isEmpty())
            return Optional.empty();

        List<PgInstanceClass> instanceClasses = provider.buildClient(account).describePgClasses();

        List<String> classIds = instanceClasses.stream().map(c -> c.detail().getSpecCode()).toList();
        List<String> classNames = instanceClasses.stream().map(PgUtil::getInstanceClassName).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeOptions(formMetaData, "specCode", classIds, classNames);

        ResizeInput input = new ResizeInput();
        input.setSpecCode(pg.get().getDBInstanceClass());
        input.setStorage(pg.get().getDBInstanceStorage());

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        PgInstanceClass instanceClass = client.describePgClass(input.getSpecCode()).orElseThrow(
                () -> new StratoException("Pg class not found")
        );

        ModifyDBInstanceSpecRequest request = new ModifyDBInstanceSpecRequest();
        request.setDBInstanceId(resource.getExternalId());

        request.setCpu(instanceClass.detail().getCPU());
        request.setMemory(instanceClass.detail().getMemory()/1024);
        request.setStorage(input.getStorage());

        request.setAutoVoucher(input.getAutoVoucher());

        request.setSwitchTag(input.getSwitchTag());

        if(Objects.equals(input.getSwitchTag(), 1L)){
            request.setSwitchStartTime(input.getSwitchTimeRange().get(0));
            request.setSwitchEndTime(input.getSwitchTimeRange().get(1));
        }

        client.modifyPgSpec(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> pg = pgHandler.describeExternalResource(account, resource.getExternalId());

        if(pg.isEmpty())
            return ResourceActionResult.finished();

        if(pg.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<DBInstance> pg = pgHandler.describePg(account, resource.getExternalId());

        if(pg.isEmpty() || PgUtil.isPrepaid(pg.get()))
            return ResourceCost.ZERO;

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        if(Utils.isBlank(input.getSpecCode()) || input.getStorage() == null)
            return ResourceCost.ZERO;

        var instanceClass = client.describePgClass(input.getSpecCode());

        if(instanceClass.isEmpty())
            return ResourceCost.ZERO;

        InquiryPriceUpgradeDBInstanceRequest request = new InquiryPriceUpgradeDBInstanceRequest();
        request.setCpu(instanceClass.get().detail().getCPU());
        request.setMemory(instanceClass.get().detail().getMemory()/1024);
        request.setStorage(input.getStorage());

        request.setDBInstanceId(resource.getExternalId());


        var response = provider.buildClient(account).describePgUpgradePrice(request);
        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price / 100.0, 1.0, ChronoUnit.HOURS);
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @SelectField(
                label = "调整后规格"
        )
        private String specCode;
        @NumberField(label = "硬盘(GB)", min = 10, step = 10, defaultValue = 100)
        private Long storage;
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

        @SelectField(
                label = "切换时间",
                options = {
                        "0",
                        "1",
                        "2"
                },
                optionNames = {
                        "升级完成时",
                        "指定时间",
                        "维护时间内"
                },
                defaultValues = "0"
        )
        private Long switchTag;
        @DateTimeField(
                label = "指定切换时间范围",
                isRange = true,
                timeOnly = true,
                conditions = "this.switchTag === '1'"
        )
        private List<String> switchTimeRange;
    }
}
