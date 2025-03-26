package com.stratocloud.provider.script.software.actions;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.provider.script.RemoteScriptService;
import com.stratocloud.provider.script.software.SoftwareHandler;
import com.stratocloud.provider.script.software.SoftwareId;
import com.stratocloud.provider.script.software.requirements.SoftwareRelationshipHandler;
import com.stratocloud.provider.script.software.requirements.SoftwareToGuestOsHandler;
import com.stratocloud.resource.*;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.script.SoftwareAction;
import com.stratocloud.script.SoftwareRequirement;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SoftwareActionHandler implements ResourceActionHandler {

    private final SoftwareHandler softwareHandler;

    private final SoftwareAction softwareAction;

    public SoftwareActionHandler(SoftwareHandler softwareHandler,
                                 SoftwareAction softwareAction) {
        this.softwareHandler = softwareHandler;
        this.softwareAction = softwareAction;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return softwareHandler;
    }

    @Override
    public ResourceAction getAction() {
        return switch (softwareAction.getActionType()){
            case INSTALL -> ResourceActions.BUILD_RESOURCE;
            case START -> ResourceActions.START;
            case STOP -> ResourceActions.STOP;
            case RESTART -> ResourceActions.RESTART;
            case UNINSTALL -> ResourceActions.DESTROY_RESOURCE;
            default -> new ResourceAction(softwareAction.getActionId(), softwareAction.getActionName(), 999);
        };
    }

    @Override
    public String getTaskName() {
        return softwareAction.getActionName();
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return switch (softwareAction.getActionType()){
            case INSTALL -> Set.of(ResourceState.NO_STATE);
            case START -> Set.of(ResourceState.STOPPED);
            case STOP -> Set.of(ResourceState.STOPPING, ResourceState.STARTED);
            case UNINSTALL -> Set.of(ResourceState.values());
            default -> ResourceState.getAliveStateSet();
        };
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return switch (softwareAction.getActionType()){
            case INSTALL -> Optional.of(ResourceState.BUILDING);
            case START -> Optional.of(ResourceState.STARTING);
            case STOP -> Optional.of(ResourceState.STOPPING);
            case UNINSTALL -> Optional.of(ResourceState.DESTROYING);
            case RESTART -> Optional.of(ResourceState.RESTARTING);
            case CONFIGURE -> Optional.of(ResourceState.CONFIGURING);
            default -> Optional.empty();
        };
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData() {
        return softwareAction.getRemoteScriptDef().getCustomForm().map(CustomForm::toDynamicFormMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        var softwareDefinition = softwareHandler.getDefinition();

        RemoteScript remoteScript = softwareAction.getRemoteScriptDef().getRemoteScript();
        Optional<CustomForm> customForm = softwareAction.getRemoteScriptDef().getCustomForm();

        Resource guestOs = resource.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Software running target not found.")
        );


        RuntimePropertiesUtil.copyManagementIpInfo(guestOs, resource);

        customForm.ifPresent(f -> RuntimePropertiesUtil.setCustomFormRuntimeProperties(
                resource, parameters, f
        ));

        Integer servicePort = softwareHandler.getDefinition().getServicePort();
        updateServicePortProperty(resource, servicePort, false);

        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(resource);

        resolveRequirementsProperties(resource, environment);

        SoftwareHandler.decryptEnvironment(softwareDefinition, environment);


        RemoteScriptResult result = remoteScriptService.execute(guestOs, remoteScript, environment);

        Map<String, String> outputArguments = result.getOutputArguments();

        RuntimePropertiesUtil.setDisplayableRuntimeProperties(resource, outputArguments);

        String managementIp = RuntimePropertiesUtil.getManagementIp(resource).orElseThrow(
                () -> new StratoException("Management IP not known.")
        );

        String outputServicePortStr = outputArguments.get("servicePort");
        if(Utils.isNotBlank(outputServicePortStr)){
            try {
                servicePort = Integer.valueOf(outputServicePortStr);
                updateServicePortProperty(resource, servicePort, true);
            } catch (Exception e) {
                log.warn("Failed to parse service port from: {}.", outputServicePortStr);
            }
        }

        servicePort = getFinalServicePort(resource).orElse(servicePort);

        resource.setName(
                "%s(%s:%s)".formatted(
                        softwareDefinition.getName(),
                        guestOs.getName(),
                        servicePort
                )
        );
        resource.setExternalId(new SoftwareId(managementIp, servicePort).toString());

        if(result.status() != RemoteScriptResult.Status.SUCCESS)
            throw new StratoException(
                    "Failed to perform action %s on software %s: %s".formatted(
                            getAction().id(), softwareDefinition.getName(), result.error()
                    )
            );
    }

    private Optional<Integer> getFinalServicePort(Resource resource) {
        var portProperty = RuntimePropertiesUtil.getRuntimePropertyByKey(resource, "servicePort");

        if(portProperty.isEmpty())
            return Optional.empty();

        String value = portProperty.get().getValue();
        try {
            return Optional.of(Integer.valueOf(value));
        }catch (Exception e){
            log.warn("Failed to parse service port from property: {}", value);
            return Optional.empty();
        }
    }

    private static void updateServicePortProperty(Resource resource, Integer servicePort, boolean force) {
        RuntimeProperty portProperty = RuntimeProperty.ofDisplayable(
                "servicePort", "服务端口", servicePort.toString(), servicePort.toString()
        );

        if(force)
            resource.addOrUpdateRuntimeProperty(portProperty);
        else
            resource.addRuntimePropertyIfAbsent(portProperty);
    }

    private void resolveRequirementsProperties(Resource resource, Map<String, String> environment) {
        List<Relationship> softwareRequirements = resource.getRequirements().stream().filter(
                rel -> rel.getTarget().isCategory(ResourceCategories.SOFTWARE)
        ).toList();

        if(Utils.isEmpty(softwareRequirements))
            return;

        Map<String, List<Relationship>> requirementsMap
                = softwareRequirements.stream().collect(Collectors.groupingBy(Relationship::getType));

        for (Map.Entry<String, List<Relationship>> entry : requirementsMap.entrySet()) {
            String relationshipTypeId = entry.getKey();

            RelationshipHandler requirementHandler = softwareHandler.getRequirement(relationshipTypeId);

            if(!(requirementHandler instanceof SoftwareRelationshipHandler softwareRelationshipHandler))
                continue;

            List<Relationship> relationships = entry.getValue();

            if(Utils.isEmpty(relationships))
                continue;

            SoftwareRequirement softwareRequirement = softwareRelationshipHandler.getSoftwareRequirement();
            String requirementKey = softwareRequirement.getRequirementKey();

            Set<String> keys
                    = RuntimePropertiesUtil.getRuntimePropertiesMap(relationships.get(0).getTarget()).keySet();

            List<Map<String, String>> targetEnvironmentList = relationships.stream().map(rel -> {
                Map<String, String> targetPropertiesMap
                        = RuntimePropertiesUtil.getRuntimePropertiesMap(rel.getTarget());

                SoftwareHandler.decryptEnvironment(softwareRequirement.getTarget(), targetPropertiesMap);

                return targetPropertiesMap;
            }).toList();

            for (String key : keys) {
                List<String> values = new ArrayList<>();
                for (Map<String, String> targetEnvironment : targetEnvironmentList) {
                    String value = targetEnvironment.get(key);

                    if(Utils.isNotBlank(value))
                        values.add(value.trim());
                }

                if(values.size() == relationships.size())
                    environment.put(requirementKey+"_"+key, String.join(" ", values));
            }
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
        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        RemoteScript remoteScript = softwareAction.getRemoteScriptDef().getRemoteScript();

        List<Resource> guestOsList = resource.getExclusiveTargetsByTargetHandler(GuestOsHandler.class);

        if(guestOsList.size() != 1)
            throw new BadCommandException("目标主机必选且只能为1个");

        Resource guestOs = guestOsList.get(0);

        remoteScriptService.validateExecutorExist(guestOs, remoteScript);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        Set<String> relTypeIds = softwareHandler.getRequirements().stream().filter(
                rel -> rel instanceof SoftwareToGuestOsHandler
        ).map(RelationshipHandler::getRelationshipTypeId).collect(Collectors.toSet());

        return List.copyOf(relTypeIds);
    }
}
