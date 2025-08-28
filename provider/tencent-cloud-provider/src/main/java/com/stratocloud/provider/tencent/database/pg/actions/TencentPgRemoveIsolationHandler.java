package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.DbActions;
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
import com.tencentcloudapi.postgres.v20170312.models.DisIsolateDBInstancesRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentPgRemoveIsolationHandler implements ResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgRemoveIsolationHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.REMOVE_ISOLATION;
    }

    @Override
    public String getTaskName() {
        return "解隔离PostgreSQL实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.SHUTDOWN);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RemoveIsolationInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<DBInstance> pg = pgHandler.describePg(account, resource.getExternalId());

        if(pg.isEmpty())
            return Optional.empty();

        RemoveIsolationInput input = new RemoveIsolationInput();
        input.setPrepaid(PgUtil.isPrepaid(pg.get()));

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(RemoveIsolationInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        RemoveIsolationInput input = JSON.convert(parameters, RemoveIsolationInput.class);

        DisIsolateDBInstancesRequest request = new DisIsolateDBInstancesRequest();

        request.setDBInstanceIdSet(new String[]{resource.getExternalId()});
        request.setAutoVoucher(input.getAutoVoucher());
        if(input.isPrepaid())
            request.setPeriod(input.getPeriod());

        client.removePgInstanceIsolation(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var pg = pgHandler.describePg(account, resource.getExternalId());

        if(pg.isEmpty())
            return ResourceActionResult.finished();

        if(Objects.equals(pg.get().getDBInstanceStatus(), "disisolating"))
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
        RemoveIsolationInput input = JSON.convert(parameters, RemoveIsolationInput.class);

        return PgUtil.getPgInstanceCost(resource, input.getPeriod() != null ? input.getPeriod() : 1L);
    }

    @Data
    public static class RemoveIsolationInput implements ResourceActionInput {
        @BooleanField(label = "是否预付费", conditions = "false")
        private boolean prepaid;
        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                        "1年", "2年", "3年"
                },
                conditions = "this.prepaid === true",
                defaultValues = "1"
        )
        private Long period;
        @BooleanField(label = "是否自动使用代金券")
        private Boolean autoVoucher;
    }
}
