package com.stratocloud.provider.script.standalone;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.SelectEntityType;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.Source;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.provider.script.RemoteScriptService;
import com.stratocloud.repository.ScriptDefinitionRepository;
import com.stratocloud.resource.*;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.JSON;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class GuestOsExecuteScriptHandler implements ResourceActionHandler {

    private final GuestOsHandler guestOsHandler;

    public GuestOsExecuteScriptHandler(GuestOsHandler guestOsHandler) {
        this.guestOsHandler = guestOsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return guestOsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.EXECUTE_SCRIPT;
    }

    @Override
    public String getTaskName() {
        return "执行脚本";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.empty();
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ExecuteScriptInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);
        ScriptDefinitionRepository scriptDefinitionRepository = ContextUtil.getBean(ScriptDefinitionRepository.class);

        ExecuteScriptInput input = JSON.convert(parameters, ExecuteScriptInput.class);

        var scriptDefinition = scriptDefinitionRepository.findById(
                input.getWrappedInput().getScriptDefinitionId()
        ).orElseThrow(() -> new StratoException("Script definition not found."));

        RemoteScript remoteScript = scriptDefinition.getRemoteScriptDef().getRemoteScript();

        Optional<CustomForm> customForm = scriptDefinition.getRemoteScriptDef().getCustomForm();

        customForm.ifPresent(f -> RuntimePropertiesUtil.setCustomFormRuntimeProperties(
                resource, input.getWrappedInput().getCustomFormData(), f
        ));


        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(resource);

        customForm.ifPresent(form -> RuntimePropertiesUtil.decryptCustomFormData(
                environment, form
        ));

        RemoteScriptResult result = remoteScriptService.execute(resource, remoteScript, environment);

        if(result.status() != RemoteScriptResult.Status.SUCCESS)
            throw new StratoException("Failed to execute script %s on %s:\n%s".formatted(
                    scriptDefinition.getName(),
                    resource.getName(),
                    result.error()
            ));
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
        ScriptDefinitionRepository scriptDefinitionRepository = ContextUtil.getBean(ScriptDefinitionRepository.class);
        ExecuteScriptInput input = JSON.convert(parameters, ExecuteScriptInput.class);

        if(input.getWrappedInput().getScriptDefinitionId() == null)
            throw new BadCommandException("请选择脚本");


        var scriptDefinition = scriptDefinitionRepository.findById(
                input.getWrappedInput().getScriptDefinitionId()
        ).orElseThrow(() -> new StratoException("Script definition not found."));

        RemoteScript remoteScript = scriptDefinition.getRemoteScriptDef().getRemoteScript();

        remoteScriptService.validateExecutorExist(resource, remoteScript);
    }

    @Data
    public static class ExecuteScriptInput implements ResourceActionInput {

        @SelectField(label = "脚本", source = Source.ENTITY, entityType = SelectEntityType.SCRIPT_DEFINITION)
        private WrappedInput wrappedInput;

        @Data
        public static class WrappedInput {
            private Long scriptDefinitionId;
            private Map<String, Object> customFormData;
        }
    }
}
