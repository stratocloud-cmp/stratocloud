package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.CreateDBInstanceForRebuildRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceDetail;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunRdsRemoveIsolationHandler implements ResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsRemoveIsolationHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.REMOVE_ISOLATION;
    }

    @Override
    public String getTaskName() {
        return "解隔离RDS实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.SHUTDOWN);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.BUILDING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RemoveIsolationInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<RdsInstance> rdsInstance = rdsHandler.describeRds(account, resource.getExternalId());

        if(rdsInstance.isEmpty())
            return Optional.empty();

        RemoveIsolationInput input = new RemoveIsolationInput();
        if(Objects.equals(rdsInstance.get().detail().getPayType(), "Prepaid")){
            input.setPayType("Prepaid");
            input.setPeriod(1L);
        }else {
            input.setPayType("Postpaid");
        }

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(RemoveIsolationInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        RemoveIsolationInput input = JSON.convert(parameters, RemoveIsolationInput.class);
        AliyunClient client = provider.buildClient(account);

        RdsInstanceDetail detail = client.rds().describeInstanceDetail(resource.getExternalId()).orElseThrow(
                () -> new StratoException("RDS instance does not exist anymore")
        );
        var attributes = detail.attributes();

        CreateDBInstanceForRebuildRequest request = new CreateDBInstanceForRebuildRequest();

        request.setDBInstanceDescription(attributes.getDBInstanceDescription());
        request.setDBInstanceId(attributes.getDBInstanceId());
        request.setDBInstanceNetType(attributes.getDBInstanceNetType());
        request.setInstanceNetworkType(attributes.getInstanceNetworkType());
        request.setPayType(input.getPayType());

        if(Objects.equals(input.getPayType(), "Prepaid")){
            if(input.getPeriod() >= 12){
                request.setPeriod("Year");
                request.setUsedTime(String.valueOf(input.getPeriod() / 12));
            }else {
                request.setPeriod("Month");
                request.setUsedTime(String.valueOf(input.getPeriod()));
            }
        }

        request.setSecurityIPList(attributes.getSecurityIPList());
        request.setVPCId(attributes.getVpcId());
        request.setVSwitchId(attributes.getVSwitchId());
        request.setZoneId(attributes.getZoneId());

        var slaveZones = attributes.getSlaveZones();

        if(slaveZones != null){
            if(Utils.length(slaveZones.getSlaveZone()) >= 1){
                request.setZoneIdSlave1(slaveZones.getSlaveZone().get(0).getZoneId());

                if(Utils.length(slaveZones.getSlaveZone()) >= 2){
                    request.setZoneIdSlave2(slaveZones.getSlaveZone().get(1).getZoneId());
                }
            }
        }

        String newInstanceId = client.rds().createInstanceForRebuild(request);

        resource.setExternalId(newInstanceId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rdsInstance = rdsHandler.describeExternalResource(account, resource.getExternalId());

        if(rdsInstance.isEmpty())
            return ResourceActionResult.failed("Instance does not exist anymore");

        if(rdsInstance.get().state() == ResourceState.BUILDING)
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

    @Data
    public static class RemoveIsolationInput implements ResourceActionInput {
        @SelectField(
                label = "计费模式",
                options = {
                        "Postpaid",
                        "Prepaid"
                },
                optionNames = {
                        "按量计费",
                        "包年包月"
                },
                defaultValues = "Postpaid"
        )
        private String payType;

        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36", "48", "60"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                        "1年", "2年", "3年", "4年", "5年"
                },
                conditions = "this.payType === 'Prepaid'",
                defaultValues = "1"
        )
        private Long period;
    }
}
