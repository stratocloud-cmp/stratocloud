package com.stratocloud.provider.aliyun.instance.requirements;

import com.aliyun.ecs20140526.models.DescribeInstanceModificationPriceRequest;
import com.aliyun.ecs20140526.models.ModifyInstanceSpecRequest;
import com.aliyun.ecs20140526.models.ModifyPrepayInstanceSpecRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorHandler;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorId;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunInstanceToFlavorHandler implements ChangeableEssentialHandler {

    private final AliyunInstanceHandler instanceHandler;

    private final AliyunFlavorHandler flavorHandler;

    public AliyunInstanceToFlavorHandler(AliyunInstanceHandler instanceHandler,
                                         AliyunFlavorHandler flavorHandler) {
        this.instanceHandler = instanceHandler;
        this.flavorHandler = flavorHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_INSTANCE_TO_FLAVOR_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与规格";
    }

    @Override
    public ResourceHandler getSource() {
        return instanceHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return flavorHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机";
    }

    @Override
    public String getRequirementName() {
        return "规格";
    }

    @Override
    public String getConnectActionName() {
        return "应用规格";
    }

    @Override
    public String getDisconnectActionName() {
        return "弃用规格";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource instance = relationship.getSource();
        Resource flavor = relationship.getTarget();

        if(instance.getState() != ResourceState.STOPPED)
            throw new BadCommandException("请先关机再执行变更规格");

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());

        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunInstance aliyunInstance = instanceHandler.describeInstance(account, instance.getExternalId()).orElseThrow(
                () -> new StratoException("Instance not found when changing flavor.")
        );

        String instanceChargeType = aliyunInstance.detail().getInstanceChargeType();

        if("PrePaid".equalsIgnoreCase(instanceChargeType)){
            ModifyPrepayInstanceSpecRequest request = new ModifyPrepayInstanceSpecRequest();

            AliyunFlavorId flavorId = AliyunFlavorId.fromString(flavor.getExternalId());

            request.setInstanceId(instance.getExternalId());
            request.setInstanceType(flavorId.instanceTypeId());

            provider.buildClient(account).ecs().modifyPrepayInstanceSpec(request);
        }else {
            ModifyInstanceSpecRequest request = new ModifyInstanceSpecRequest();

            AliyunFlavorId flavorId = AliyunFlavorId.fromString(flavor.getExternalId());

            request.setInstanceId(instance.getExternalId());
            request.setInstanceType(flavorId.instanceTypeId());


            provider.buildClient(account).ecs().modifyInstanceSpec(request);
        }
    }

    @Override
    public Set<ResourceState> getAllowedSourceStates() {
        return Set.of(ResourceState.STOPPED);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        AliyunFlavorId flavorId = new AliyunFlavorId(
                instance.get().detail().getZoneId(),
                instance.get().detail().getInstanceType()
        );

        Optional<ExternalResource> flavor
                = flavorHandler.describeExternalResource(account, flavorId.toString());

        if(flavor.isEmpty())
            return List.of();

        ExternalRequirement flavorRequirement = new ExternalRequirement(
                getRelationshipTypeId(),
                flavor.get(),
                Map.of()
        );

        return List.of(flavorRequirement);
    }

    @Override
    public ResourceCost getChangeCost(Resource source, Resource newTarget, Map<String, Object> relationshipInputs) {
        if(Utils.isBlank(source.getExternalId()) || Utils.isBlank(newTarget.getExternalId()))
            return ResourceCost.ZERO;

        AliyunFlavorId flavorId = AliyunFlavorId.fromString(newTarget.getExternalId());


        ExternalAccount account = getAccountRepository().findExternalAccount(source.getAccountId());

        AliyunInstance instance = instanceHandler.describeInstance(account, source.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found.")
        );

        AliyunCloudProvider provider = (AliyunCloudProvider) getSource().getProvider();

        if(!Objects.equals("PrePaid", instance.detail().getInstanceChargeType()))
            return ResourceCost.ZERO;

        var request = new DescribeInstanceModificationPriceRequest();
        request.setInstanceId(source.getExternalId());
        request.setInstanceType(flavorId.instanceTypeId());

        Float tradePrice = provider.buildClient(account).ecs().describeInstanceModificationPrice(
                request
        ).getPriceInfo().getPrice().getTradePrice();

        LocalDateTime expiredTime = AliyunTimeUtil.toLocalDateTime(instance.detail().getExpiredTime());
        long months = ChronoUnit.MONTHS.between(LocalDateTime.now(), expiredTime);

        return new ResourceCost(tradePrice, months, ChronoUnit.MONTHS);
    }
}
