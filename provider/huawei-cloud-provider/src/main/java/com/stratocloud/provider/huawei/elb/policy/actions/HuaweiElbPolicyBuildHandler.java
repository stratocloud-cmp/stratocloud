package com.stratocloud.provider.huawei.elb.policy.actions;

import com.huaweicloud.sdk.elb.v3.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.elb.policy.HuaweiElbPolicyHandler;
import com.stratocloud.provider.huawei.elb.policy.requirements.HuaweiElbPolicyToRedirectListenerHandler;
import com.stratocloud.provider.huawei.elb.policy.requirements.HuaweiElbPolicyToRedirectPoolHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiElbPolicyBuildHandler implements BuildResourceActionHandler {

    private final HuaweiElbPolicyHandler policyHandler;

    public HuaweiElbPolicyBuildHandler(HuaweiElbPolicyHandler policyHandler) {
        this.policyHandler = policyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "创建转发策略";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiElbPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiElbPolicyBuildInput input = JSON.convert(parameters, HuaweiElbPolicyBuildInput.class);

        String action = input.getAction();

        CreateL7PolicyOption option = new CreateL7PolicyOption();
        option.withName(resource.getName()).withDescription(resource.getDescription());
        option.withAction(input.getAction());

        Resource listenerResource = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER).orElseThrow(
                () -> new StratoException("Listener not provided when creating policy")
        );

        HuaweiCloudProvider provider = (HuaweiCloudProvider) policyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);
        Listener listener = client.elb().describeListener(
                listenerResource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Listener not found when creating policy")
        );

        option.withListenerId(listener.getId());

        Boolean enhanceL7policyEnable = listener.getEnhanceL7policyEnable();

        if(input.getPriority() != null)
            if(enhanceL7policyEnable != null && enhanceL7policyEnable)
                option.withPriority(input.getPriority());

        switch (action) {
            case "REDIRECT_TO_POOL" -> resolveRedirectPoolsConfig(resource, option);
            case "REDIRECT_TO_LISTENER" -> resolveRedirectListenerConfig(resource, option);
            case "REDIRECT_TO_URL" -> resolveRedirectUrlConfig(input, option);
            case "FIXED_RESPONSE" -> resolveFixedResponse(input, option);
        }

        String policyId = client.elb().createPolicy(new CreateL7PolicyRequest().withBody(
                new CreateL7PolicyRequestBody().withL7policy(option)
        ));

        resource.setExternalId(policyId);
    }

    private void resolveFixedResponse(HuaweiElbPolicyBuildInput input, CreateL7PolicyOption option) {
        CreateFixtedResponseConfig fixedResponseConfig = new CreateFixtedResponseConfig();

        fixedResponseConfig.withStatusCode(input.getFixedStatusCode());

        if(Utils.isNotBlank(input.getFixedContentType()))
            fixedResponseConfig.withContentType(
                    CreateFixtedResponseConfig.ContentTypeEnum.fromValue(input.getFixedContentType())
            );

        if(Utils.isNotBlank(input.getFixedMessageBody()))
            fixedResponseConfig.withMessageBody(input.getFixedMessageBody());

        option.withFixedResponseConfig(fixedResponseConfig);
    }

    private void resolveRedirectUrlConfig(HuaweiElbPolicyBuildInput input, CreateL7PolicyOption option) {
        CreateRedirectUrlConfig urlConfig = new CreateRedirectUrlConfig();

        if(Utils.isNotBlank(input.getRedirectUrlProtocol()))
            urlConfig.withProtocol(
                    CreateRedirectUrlConfig.ProtocolEnum.fromValue(
                            input.getRedirectUrlProtocol()
                    )
            );

        if(Utils.isNotBlank(input.getRedirectUrlHost()))
            urlConfig.withHost(input.getRedirectUrlHost());

        if(input.getRedirectUrlPort() != null)
            urlConfig.withPort(input.getRedirectUrlPort().toString());

        if(Utils.isNotBlank(input.getRedirectUrlPath()))
            urlConfig.withPath(input.getRedirectUrlPath());

        if(Utils.isNotBlank(input.getRedirectUrlQuery()))
            urlConfig.withQuery(input.getRedirectUrlQuery());

        if(Utils.isNotEmpty(input.getInsertHeadersConfig()))
            urlConfig.withInsertHeadersConfig(
                    convertInsertHeadersConfig(input.getInsertHeadersConfig())
            );

        if(Utils.isNotEmpty(input.getRemoveHeadersConfig()))
            urlConfig.withRemoveHeadersConfig(
                    convertRemoveHeadersConfig(input.getRemoveHeadersConfig())
            );

        urlConfig.withStatusCode(
                CreateRedirectUrlConfig.StatusCodeEnum.fromValue(input.getRedirectUrlStatusCode())
        );

        option.withRedirectUrlConfig(urlConfig);
    }

    private CreateRemoveHeadersConfig convertRemoveHeadersConfig(List<String> removeHeadersConfig) {
        CreateRemoveHeadersConfig config = new CreateRemoveHeadersConfig();

        for (String removeHeader : removeHeadersConfig) {
            config.addConfigsItem(new CreateRemoveHeaderConfig().withKey(removeHeader));
        }

        return config;
    }

    private CreateInsertHeadersConfig convertInsertHeadersConfig(List<String> insertHeadersConfig) {
        CreateInsertHeadersConfig config = new CreateInsertHeadersConfig();

        for (String insertHeader : insertHeadersConfig) {
            if(Utils.isBlank(insertHeader))
                continue;

            InsertingHeader insertingHeader = JSON.toJavaObject(insertHeader, InsertingHeader.class);

            config.addConfigsItem(
                    new CreateInsertHeaderConfig().withKey(
                            insertingHeader.key()
                    ).withValue(
                            insertingHeader.value()
                    ).withValueType(
                            insertingHeader.valueType()
                    )
            );
        }

        return config;
    }

    private void resolveRedirectListenerConfig(Resource resource, CreateL7PolicyOption option) {
        Resource redirectListener = resource.getEssentialTargetByType(
                HuaweiElbPolicyToRedirectListenerHandler.TYPE_ID
        ).orElseThrow(
                () -> new StratoException("Redirect listener not provided")
        );

        option.withRedirectListenerId(redirectListener.getExternalId());
    }

    private void resolveRedirectPoolsConfig(Resource resource, CreateL7PolicyOption option) {
        List<Relationship> relationships = resource.getRequirements().stream().filter(
                rel -> Objects.equals(
                        HuaweiElbPolicyToRedirectPoolHandler.TYPE_ID, rel.getType()
                )
        ).toList();

        if(Utils.isEmpty(relationships))
            return;

        List<CreateRedirectPoolsConfig> poolsConfigs = new ArrayList<>();

        for (Relationship relationship : relationships) {
            var poolConfigInput = JSON.convert(
                    relationship.getProperties(),
                    HuaweiElbPolicyToRedirectPoolHandler.PoolConfigInput.class
            );

            var config = new CreateRedirectPoolsConfig().withPoolId(
                    relationship.getTarget().getExternalId()
            );

            if(poolConfigInput.getWeight() != null)
                config.withWeight(
                        poolConfigInput.getWeight().toString()
                );

            poolsConfigs.add(config);
        }

        option.withRedirectPoolsConfig(poolsConfigs);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        HuaweiElbPolicyBuildInput input = JSON.convert(parameters, HuaweiElbPolicyBuildInput.class);

        Resource listenerResource = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER).orElseThrow(
                () -> new StratoException("Listener not provided when creating policy")
        );

        HuaweiCloudProvider provider = (HuaweiCloudProvider) policyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Listener listener = provider.buildClient(account).elb().describeListener(
                listenerResource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Listener not found when creating policy")
        );

        Boolean enhanceL7policyEnable = listener.getEnhanceL7policyEnable();

        if(enhanceL7policyEnable == null || !enhanceL7policyEnable)
            if("REDIRECT_TO_URL".equals(input.getAction()) || "FIXED_RESPONSE".equals(input.getAction()))
                throw new BadCommandException("监听器未开启高级转发策略，不支持重定向到URL或返回固定响应体");

        if("REDIRECT_TO_POOL".equals(input.getAction())){
            Optional<Resource> redirectListener = resource.getEssentialTargetByType(
                    HuaweiElbPolicyToRedirectListenerHandler.TYPE_ID
            );

            if(redirectListener.isPresent())
                throw new BadCommandException("已选择重定向到后端服务器组，无法同时重定向到监听器");
        }

        if("REDIRECT_TO_LISTENER".equals(input.getAction())){
            Optional<Resource> redirectListener = resource.getEssentialTargetByType(
                    HuaweiElbPolicyToRedirectListenerHandler.TYPE_ID
            );

            if(redirectListener.isEmpty())
                throw new BadCommandException("未选择重定向的监听器");

            List<Relationship> poolRelList = resource.getRequirements().stream().filter(
                    rel -> HuaweiElbPolicyToRedirectPoolHandler.TYPE_ID.equals(rel.getType())
            ).toList();

            if(Utils.isNotEmpty(poolRelList))
                throw new BadCommandException("已选择重定向到监听器，无法同时重定向到后端服务器组");
        }
    }
}
