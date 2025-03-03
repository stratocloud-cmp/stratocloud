package com.stratocloud.provider.aliyun.instance.actions;

import com.aliyun.ecs20140526.models.ModifyInstanceAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class AliyunInstanceUpdateHandler implements ResourceActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceUpdateHandler(AliyunInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云主机";
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
    public void run(Resource resource, Map<String, Object> parameters) {
        UpdateInput updateInput = JSON.convert(parameters, UpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        ExternalResource instance = instanceHandler.describeExternalResource(
                account,
                resource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Instance not found when updating attributes.")
        );

        ModifyInstanceAttributeRequest request = new ModifyInstanceAttributeRequest();

        request.setInstanceId(instance.externalId());

        if(updateInput.getUpdatingInstanceName())
            request.setInstanceName(updateInput.getInstanceName());

        if(updateInput.getUpdatingHostName())
            request.setHostName(updateInput.getHostName());

        if(updateInput.getUpdatingPassword())
            request.setPassword(updateInput.getPassword());

        if(updateInput.getUpdatingDescription())
            request.setDescription(updateInput.getDescription());



        provider.buildClient(account).ecs().modifyInstance(request);

        if(updateInput.getUpdatingDescription())
            resource.setDescription(updateInput.getDescription());
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
    public static class UpdateInput implements ResourceActionInput{
        @BooleanField(label = "是否更新实例名称")
        private Boolean updatingInstanceName = false;
        @InputField(label = "新实例名称", conditions = "this.updatingInstanceName === true")
        private String instanceName;
        @BooleanField(label = "是否更新操作系统的主机名")
        private Boolean updatingHostName = false;
        @InputField(label = "操作系统的新主机名", conditions = "this.updatingHostName === true")
        private String hostName;
        @BooleanField(label = "是否重置密码")
        private Boolean updatingPassword = false;
        @InputField(label = "新密码", inputType = "password", conditions = "this.updatingPassword === true")
        private String password;
        @BooleanField(label = "是否更新描述")
        private Boolean updatingDescription = false;
        @InputField(label = "新描述", inputType = "textarea", conditions = "this.updatingDescription === true")
        private String description;
    }
}
