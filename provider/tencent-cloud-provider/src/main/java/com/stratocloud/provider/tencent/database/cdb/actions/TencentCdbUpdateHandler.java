package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
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
import com.stratocloud.provider.tencent.database.cdb.CdbArchitecture;
import com.stratocloud.provider.tencent.database.cdb.CdbUtil;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCdbUpdateHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbUpdateHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云数据库基本信息";
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
        return UpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<InstanceInfo> cdb = cdbHandler.describeCdb(account, resource.getExternalId());
        if(cdb.isEmpty())
            return Optional.empty();

        UpdateInput updateInput = new UpdateInput();
        updateInput.setArchitecture(CdbArchitecture.fromCdb(cdb.get()));
        updateInput.setPrepaid(CdbUtil.isPrepaid(cdb.get()));
        updateInput.setInstanceName(cdb.get().getInstanceName());
        updateInput.setProtectMode(cdb.get().getProtectMode());
        updateInput.setAutoRenew(cdb.get().getAutoRenew());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);

        return Optional.ofNullable(
                DynamicFormHelper.changeDefaultValues(formMetaData, updateInput)
        );
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        InstanceInfo cdb = cdbHandler.describeCdb(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("CDB not found")
        );
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        if(!Objects.equals(cdb.getInstanceName(), input.getInstanceName())){
            client.modifyCdbName(cdb.getInstanceId(), input.getInstanceName());
        }

        CdbArchitecture architecture = input.getArchitecture();
        if(architecture != CdbArchitecture.ONE_NODE && !Objects.equals(cdb.getProtectMode(), input.getProtectMode())){
            client.modifyCdbProtectMode(cdb.getInstanceId(), input.getProtectMode());
        }

        if(input.isPrepaid() && !Objects.equals(cdb.getAutoRenew(), input.getAutoRenew())){
            client.modifyCdbAutoRenewFlag(cdb.getInstanceId(), input.getAutoRenew());
        }
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

    }

    @Data
    public static class UpdateInput implements ResourceActionInput {
        @SelectField(label = "架构", conditions = "false")
        private CdbArchitecture architecture;
        @BooleanField(label = "是否预付费", conditions = "false")
        private boolean prepaid;

        @InputField(label = "实例名称")
        private String instanceName;

        @SelectField(
                label = "数据复制方式",
                options = {
                        "0",
                        "1",
                        "2"
                },
                optionNames = {
                        "异步复制",
                        "半同步复制",
                        "强同步复制"
                },
                defaultValues = "1",
                conditions = "this.architecture !== 'ONE_NODE'"
        )
        private Long protectMode;

        @SelectField(
                label = "自动续费",
                conditions = "this.prepaid === true",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "否",
                        "是"
                }
        )
        private Long autoRenew;
    }
}
