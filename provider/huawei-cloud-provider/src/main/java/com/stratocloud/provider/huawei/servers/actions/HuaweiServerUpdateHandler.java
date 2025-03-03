package com.stratocloud.provider.huawei.servers.actions;

import com.huaweicloud.sdk.ecs.v2.model.UpdateServerOption;
import com.huaweicloud.sdk.ecs.v2.model.UpdateServerRequest;
import com.huaweicloud.sdk.ecs.v2.model.UpdateServerRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiServerUpdateHandler implements ResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerUpdateHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "修改云主机";
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
        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        UpdateServerOption option = new UpdateServerOption();

        if(Utils.isNotBlank(input.getName()))
            option.withName(input.getName());

        if(Utils.isNotBlank(input.getDescription()))
            option.withDescription(input.getDescription());

        if(Utils.isNotBlank(input.getUserData()))
            option.withUserData(SecurityUtil.encodeToBase64(input.getUserData()));

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).ecs().updateServer(
                new UpdateServerRequest().withServerId(resource.getExternalId()).withBody(
                        new UpdateServerRequestBody().withServer(option)
                )
        );
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
    public static class UpdateInput implements ResourceActionInput {
        @InputField(label = "主机名", required = false)
        private String name;
        @InputField(label = "描述", required = false)
        private String description;
        @InputField(
                label = "自定义数据(user_data)",
                inputType = "textarea",
                required = false
        )
        private String userData;
    }
}
