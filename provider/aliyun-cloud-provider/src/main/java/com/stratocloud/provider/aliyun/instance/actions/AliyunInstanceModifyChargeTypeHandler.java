package com.stratocloud.provider.aliyun.instance.actions;

import com.aliyun.ecs20140526.models.ModifyInstanceChargeTypeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AliyunInstanceModifyChargeTypeHandler implements ResourceActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceModifyChargeTypeHandler(AliyunInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.MODIFY_CHARGE_TYPE;
    }

    @Override
    public String getTaskName() {
        return "云主机变更付费方式";
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
        return AliyunInstanceModifyChargeTypeInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        performChargeTypeModification(resource, parameters, false);
    }

    private void performChargeTypeModification(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunInstance instance = instanceHandler.describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Instance not found when modifying charge type.")
        );


        var input = JSON.convert(parameters, AliyunInstanceModifyChargeTypeInput.class);

        if(Objects.equals(input.getChargeType(), instance.detail().getInstanceChargeType())){
            log.warn("Instance {} is already being charged by {}, skipping...",
                    instance.detail().getInstanceName(), input.getChargeType());
            return;
        }

        ModifyInstanceChargeTypeRequest request = new ModifyInstanceChargeTypeRequest();
        request.setAutoPay(true);
        request.setDryRun(dryRun);
        request.setIncludeDataDisks(false);
        request.setInstanceChargeType(input.getChargeType());
        request.setInstanceIds(JSON.toJsonString(List.of(instance.detail().getInstanceId())));
        request.setIsDetailFee(true);

        if("PrePaid".equalsIgnoreCase(input.getChargeType())){
            request.setPeriod(input.getPrepaidPeriod());
            request.setPeriodUnit("Month");
        }

        provider.buildClient(account).ecs().modifyInstanceChargeType(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunInstance instance = instanceHandler.describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Instance not found after modified charge type.")
        );


        var input = JSON.convert(parameters, AliyunInstanceModifyChargeTypeInput.class);

        if(Objects.equals(input.getChargeType(), instance.detail().getInstanceChargeType())){
            return ResourceActionResult.finished();
        }else {
            return ResourceActionResult.failed("Failed to modify charge type of %s".formatted(resource.getName()));
        }
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        performChargeTypeModification(resource, parameters, true);
    }
}
