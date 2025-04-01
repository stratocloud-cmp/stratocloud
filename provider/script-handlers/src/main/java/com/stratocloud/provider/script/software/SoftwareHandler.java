package com.stratocloud.provider.script.software;


import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.provider.script.RemoteScriptService;
import com.stratocloud.provider.script.software.actions.SoftwareActionHandler;
import com.stratocloud.resource.*;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.script.SoftwareAction;
import com.stratocloud.script.SoftwareActionType;
import com.stratocloud.script.SoftwareDefinition;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SoftwareHandler extends AbstractResourceHandler implements DynamicResourceHandler {

    private final Provider provider;

    private final SoftwareDefinition definition;

    public SoftwareHandler(Provider provider,
                           SoftwareDefinition definition) {
        this.provider = provider;
        this.definition = definition;

        if(Utils.isNotEmpty(definition.getActions())){
            for (SoftwareAction softwareAction : definition.getActions()) {
                SoftwareActionHandler actionHandler = new SoftwareActionHandler(this, softwareAction);
                registerActionHandler(actionHandler);
            }
        }
    }



    public SoftwareDefinition getDefinition() {
        return definition;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return definition.generateSoftwareResourceTypeId(provider.getId());
    }



    @Override
    public String getResourceTypeName() {
        return definition.getName();
    }


    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SOFTWARE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return Optional.of(getExternalResource(account, externalId));
    }

    private ExternalResource getExternalResource(ExternalAccount account, String externalId) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                externalId,
                externalId,
                ResourceState.UNKNOWN
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {
        var remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        Optional<SoftwareAction> softwareAction = definition.getActionByType(SoftwareActionType.CHECK_STATE);

        if(softwareAction.isEmpty()){
            log.warn("No CHECK_STATE action for software {}.", resource.getName());
            resource.setState(ResourceState.UNKNOWN);
            return;
        }

        Resource guestOsResource = resource.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Guest os resource not found when synchronizing software")
        );

        RemoteScript remoteScript = softwareAction.get().getRemoteScriptDef().getRemoteScript();

        RuntimePropertiesUtil.copyManagementIpInfo(guestOsResource, resource);

        Map<String, String> runtimePropertiesMap = RuntimePropertiesUtil.getRuntimePropertiesMap(resource);

        decryptEnvironment(definition, runtimePropertiesMap);

        RemoteScriptResult result = remoteScriptService.execute(guestOsResource, remoteScript, runtimePropertiesMap);

        if(result.status() == RemoteScriptResult.Status.FAILED)
            throw new StratoException(result.output());

        Map<String, String> outputArguments = result.getOutputArguments();

        RuntimePropertiesUtil.setDisplayableRuntimeProperties(resource, outputArguments);

        if(outputArguments.containsKey("serviceState")){
            String serviceState = outputArguments.get("serviceState");

            if(Objects.equals(serviceState, "STARTED"))
                resource.setState(ResourceState.STARTED);
            else if(Objects.equals(serviceState, "STOPPED"))
                resource.setState(ResourceState.STOPPED);
            else if(Objects.equals(serviceState, "ERROR"))
                resource.setState(ResourceState.ERROR);
            else
                log.warn("Unrecognized service state {} of software {}.", serviceState, resource.getName());
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }


    public static void decryptEnvironment(SoftwareDefinition softwareDefinition, Map<String, String> environment) {
        for (SoftwareAction action : softwareDefinition.getActions()) {
            Optional<CustomForm> eachForm = action.getRemoteScriptDef().getCustomForm();
            eachForm.ifPresent(form -> RuntimePropertiesUtil.decryptCustomFormData(
                    environment, form
            ));
        }
    }
}
