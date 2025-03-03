package com.stratocloud.provider.tencent.instance.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.flavor.TencentFlavorHandler;
import com.stratocloud.provider.tencent.flavor.TencentFlavorId;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.InquiryPriceResetInstancesTypeRequest;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.Price;
import com.tencentcloudapi.cvm.v20170312.models.ResetInstancesTypeRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentInstanceToFlavorHandler implements ChangeableEssentialHandler {

    private final TencentInstanceHandler instanceHandler;

    private final TencentFlavorHandler flavorHandler;

    public TencentInstanceToFlavorHandler(TencentInstanceHandler instanceHandler, TencentFlavorHandler flavorHandler) {
        this.instanceHandler = instanceHandler;
        this.flavorHandler = flavorHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_INSTANCE_TO_FLAVOR_RELATIONSHIP";
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

        ChangeFlavorInput input = JSON.convert(relationship.getProperties(), ChangeFlavorInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());

        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        ResetInstancesTypeRequest request = new ResetInstancesTypeRequest();

        TencentFlavorId flavorId = TencentFlavorId.fromString(flavor.getExternalId());

        request.setInstanceIds(new String[]{instance.getExternalId()});
        request.setInstanceType(flavorId.instanceType());
        request.setForceStop(input.getForceStop());

        provider.buildClient(account).resetInstanceType(request);
    }



    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        if(isConnected(relationship, account))
            return RelationshipActionResult.finished();

        return TencentInstanceUtil.checkLastOperationStateForRelationship(
                instanceHandler, account, relationship.getSource()
        );
    }

    @Override
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return ChangeFlavorInput.class;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Instance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        TencentFlavorId flavorId = new TencentFlavorId(
                instance.get().getPlacement().getZone(),
                instance.get().getInstanceType()
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

        TencentFlavorId flavorId = TencentFlavorId.fromString(newTarget.getExternalId());

        var request = new InquiryPriceResetInstancesTypeRequest();
        request.setInstanceIds(new String[]{source.getExternalId()});
        request.setInstanceType(flavorId.instanceType());

        ExternalAccount account = getAccountRepository().findExternalAccount(source.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) getSource().getProvider();

        Price price = provider.buildClient(account).inquiryPriceResetInstanceType(request);

        Instance instance = instanceHandler.describeInstance(account, source.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found.")
        );


        switch (instance.getInstanceChargeType()){
            case "PREPAID" -> {
                LocalDateTime expiredTime = TencentTimeUtil.toLocalDateTime(instance.getExpiredTime());
                long months = ChronoUnit.MONTHS.between(LocalDateTime.now(), expiredTime);
                return new ResourceCost(price.getInstancePrice().getDiscountPrice(), months, ChronoUnit.MONTHS);
            }
            case "POSTPAID_BY_HOUR", "SPOTPAID" -> {
                return new ResourceCost(
                        price.getInstancePrice().getUnitPriceDiscount(),
                        1.0,
                        ChronoUnit.HOURS
                );
            }
            default -> {
                return ResourceCost.ZERO;
            }
        }
    }

    @Data
    public static class ChangeFlavorInput implements RelationshipConnectInput{
        @BooleanField(label = "强制关机")
        private Boolean forceStop;
    }
}
