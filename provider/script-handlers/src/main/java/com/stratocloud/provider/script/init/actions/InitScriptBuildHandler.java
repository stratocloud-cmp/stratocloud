package com.stratocloud.provider.script.init.actions;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.provider.script.RemoteScriptService;
import com.stratocloud.provider.script.init.InitScriptHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.resource.RuntimeProperty;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InitScriptBuildHandler implements BuildResourceActionHandler {

    private final InitScriptHandler scriptHandler;


    public InitScriptBuildHandler(InitScriptHandler scriptHandler) {
        this.scriptHandler = scriptHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return scriptHandler;
    }

    @Override
    public String getTaskName() {
        return "执行"+scriptHandler.getDefinition().getName();
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData() {
        return scriptHandler.getDefinition().getRemoteScriptDef().getCustomForm().map(CustomForm::toDynamicFormMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        var scriptDefinition = scriptHandler.getDefinition();

        RemoteScript remoteScript = scriptDefinition.getRemoteScriptDef().getRemoteScript();

        Optional<CustomForm> customForm = scriptDefinition.getRemoteScriptDef().getCustomForm();

        Resource guestOs = resource.getExclusiveTargetByTargetHandler(GuestOsHandler.class).orElseThrow(
                () -> new StratoException("Script running target not found.")
        );

        resource.setName("%s (%s)".formatted(scriptDefinition.getName(), guestOs.getName()));

        RuntimePropertiesUtil.copyManagementIpInfo(guestOs, resource);


        customForm.ifPresent(f -> RuntimePropertiesUtil.setCustomFormRuntimeProperties(
                resource, parameters, f
        ));


        Map<String, String> environment = RuntimePropertiesUtil.getRuntimePropertiesMap(resource);

        customForm.ifPresent(form -> RuntimePropertiesUtil.decryptCustomFormData(
                environment, form
        ));

        RemoteScriptResult result = remoteScriptService.execute(guestOs, remoteScript, environment);

        resource.setExternalId(resource.getId().toString());

        if(Utils.isNotBlank(result.output())){
            RuntimeProperty outputProperty = RuntimeProperty.ofDisplayInList(
                    "output", "执行结果", result.output(), result.output()
            );
            resource.addOrUpdateRuntimeProperty(outputProperty);
        }

        if(Utils.isNotBlank(result.error())){
            RuntimeProperty errorProperty = RuntimeProperty.ofDisplayable(
                    "error", "错误", result.error(), result.error()
            );
            resource.addOrUpdateRuntimeProperty(errorProperty);
        }


        if(result.status() == RemoteScriptResult.Status.SUCCESS)
            TaskContext.setMessage(result.output());
        else
            throw new StratoException("Failed to execute script %s on %s:\n%s".formatted(
                    scriptDefinition.getName(),
                    guestOs.getName(),
                    result.output()
            ));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        RemoteScriptService remoteScriptService = ContextUtil.getBean(RemoteScriptService.class);

        var scriptDefinition = scriptHandler.getDefinition();

        RemoteScript remoteScript = scriptDefinition.getRemoteScriptDef().getRemoteScript();

        List<Resource> guestOsList = resource.getExclusiveTargetsByTargetHandler(GuestOsHandler.class);

        if(guestOsList.size() != 1)
            throw new BadCommandException("目标主机必选且只能为1个");

        Resource guestOs = guestOsList.get(0);

        remoteScriptService.validateExecutorExist(guestOs, remoteScript);
    }
}
