package com.stratocloud.provider.script.software.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.provider.script.RemoteScriptService;
import com.stratocloud.provider.script.software.SoftwareHandler;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.script.RemoteScriptDef;
import com.stratocloud.script.SoftwareRequirement;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


@Slf4j
public class SoftwareRelationshipHandler implements RelationshipHandler {

    private final SoftwareHandler sourceSoftwareHandler;

    private final SoftwareHandler targetSoftwareHandler;

    private final SoftwareRequirement softwareRequirement;

    public SoftwareRelationshipHandler(SoftwareHandler sourceSoftwareHandler,
                                       SoftwareHandler targetSoftwareHandler,
                                       SoftwareRequirement softwareRequirement) {
        this.sourceSoftwareHandler = sourceSoftwareHandler;
        this.targetSoftwareHandler = targetSoftwareHandler;
        this.softwareRequirement = softwareRequirement;
    }

    public SoftwareRequirement getSoftwareRequirement() {
        return softwareRequirement;
    }

    @Override
    public String getRelationshipTypeId() {
        return "%s_TO_%s_%s_RELATIONSHIP".formatted(
                sourceSoftwareHandler.getResourceTypeId(),
                targetSoftwareHandler.getResourceTypeId(),
                softwareRequirement.getRequirementKey()
        );
    }

    @Override
    public String getRelationshipTypeName() {
        return "%s与%s".formatted(
                targetSoftwareHandler.getResourceTypeName(), sourceSoftwareHandler.getResourceTypeName()
        );
    }

    @Override
    public ResourceHandler getSource() {
        return sourceSoftwareHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return targetSoftwareHandler;
    }

    @Override
    public String getCapabilityName() {
        return softwareRequirement.getCapabilityName();
    }

    @Override
    public String getRequirementName() {
        return softwareRequirement.getRequirementName();
    }

    @Override
    public String getConnectActionName() {
        return "添加";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除";
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectConnectInputClassFormMetaData() {
        return softwareRequirement.getConnectScriptDef().getCustomForm().map(CustomForm::toDynamicFormMetaData);
    }

    @Override
    public void connect(Relationship relationship) {
        Resource sourceSoftware = relationship.getSource();
        Resource targetSoftware = relationship.getTarget();

        Resource guestOs = sourceSoftware.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Guest os resource not found.")
        );

        RemoteScriptDef connectScriptDef = softwareRequirement.getConnectScriptDef();

        connectScriptDef.getCustomForm().ifPresent(f -> RuntimePropertiesUtil.setCustomFormRuntimeProperties(
                sourceSoftware, relationship.getProperties(), f
        ));

        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(sourceSoftware);

        resolveRequirementProperties(targetSoftware, environment);

        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);


        RemoteScriptResult result = remoteScriptService.execute(
                guestOs, connectScriptDef.getRemoteScript(), environment
        );

        if(result.status() != RemoteScriptResult.Status.SUCCESS)
            throw new StratoException("Failed to connect software relationship %s.".formatted(getRelationshipTypeId()));
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource sourceSoftware = relationship.getSource();
        Resource targetSoftware = relationship.getTarget();

        Resource guestOs = sourceSoftware.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Guest os resource not found.")
        );

        RemoteScriptDef disconnectScriptDef = softwareRequirement.getDisconnectScriptDef();

        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(sourceSoftware);

        resolveRequirementProperties(targetSoftware, environment);

        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        RemoteScriptResult result = remoteScriptService.execute(
                guestOs, disconnectScriptDef.getRemoteScript(), environment
        );

        if(result.status() != RemoteScriptResult.Status.SUCCESS)
            throw new StratoException("Failed to disconnect software relationship %s.".formatted(getRelationshipTypeId()));
    }

    private void resolveRequirementProperties(Resource requirementTarget, Map<String, String> environment) {
        Map<String, String> requirementProperties = RuntimePropertiesUtil.getRuntimePropertiesMap(
                requirementTarget
        );

        if(Utils.isNotEmpty(requirementProperties)){
            for (var entry : requirementProperties.entrySet()) {
                environment.put(
                        softwareRequirement.getTarget().getDefinitionKey()+"_"+entry.getKey(), entry.getValue()
                );
            }
        }
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        ResourceRepository resourceRepository = ContextUtil.getBean(ResourceRepository.class);

        Optional<Resource> resource = resourceRepository.findByExternalResource(source);

        if(resource.isEmpty())
            return List.of();

        Resource sourceSoftware = resource.get();

        List<String> targetIds = retrieveTargetIds(sourceSoftware);


        List<ExternalRequirement> result = new ArrayList<>();

        if(Utils.isNotEmpty(targetIds)){
            for (String targetId : targetIds) {
                ExternalResource externalTarget = source.toBuilder().externalId(targetId).type(
                        targetSoftwareHandler.getResourceTypeId()
                ).build();

                result.add(
                        new ExternalRequirement(getRelationshipTypeId(), externalTarget, Map.of())
                );
            }
        }

        return result;
    }

    @Override
    public boolean disconnectOnLost() {
        return true;
    }

    private List<String> retrieveTargetIds(Resource software){
        Resource guestOs = software.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Guest os resource not found.")
        );

        RemoteScriptDef checkConnectionScriptDef = softwareRequirement.getCheckConnectionScriptDef();

        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(software);

        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        RemoteScriptResult result = remoteScriptService.execute(
                guestOs, checkConnectionScriptDef.getRemoteScript(), environment
        );

        Map<String, String> outputArguments = result.getOutputArguments();

        if(Utils.isEmpty(outputArguments))
            return List.of();

        String targetIdsStr = outputArguments.get("targetIds");

        if(Utils.isBlank(targetIdsStr))
            return List.of();

        return List.of(targetIdsStr.split(","));
    }

    @Override
    public boolean visibleInTarget() {
        return softwareRequirement.getSource().isVisibleInTarget();
    }
}
