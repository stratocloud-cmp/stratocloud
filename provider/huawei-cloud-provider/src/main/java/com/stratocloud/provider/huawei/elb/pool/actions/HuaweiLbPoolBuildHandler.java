package com.stratocloud.provider.huawei.elb.pool.actions;

import com.huaweicloud.sdk.elb.v3.model.CreatePoolOption;
import com.huaweicloud.sdk.elb.v3.model.CreatePoolRequest;
import com.huaweicloud.sdk.elb.v3.model.CreatePoolRequestBody;
import com.huaweicloud.sdk.elb.v3.model.CreatePoolSessionPersistenceOption;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class HuaweiLbPoolBuildHandler implements BuildResourceActionHandler {

    private final HuaweiLbPoolHandler lbPoolHandler;

    public HuaweiLbPoolBuildHandler(HuaweiLbPoolHandler lbPoolHandler) {
        this.lbPoolHandler = lbPoolHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return lbPoolHandler;
    }

    @Override
    public String getTaskName() {
        return "创建后端服务器组";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiLbPoolBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiLbPoolBuildInput input = JSON.convert(parameters, HuaweiLbPoolBuildInput.class);

        Resource listener = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER).orElseThrow(
                () -> new StratoException("Listener not found when creating LB pool.")
        );

        CreatePoolOption poolOption = new CreatePoolOption()
                .withName(resource.getName())
                .withDescription(resource.getDescription())
                .withProtocol(input.getProtocol())
                .withLbAlgorithm(input.getLbAlgorithm())
                .withListenerId(listener.getExternalId());

        if(input.isEnableSessionPersistence()){
            var persistenceType = CreatePoolSessionPersistenceOption.TypeEnum.fromValue(
                    input.getSessionPersistenceType()
            );
            var persistenceOption = new CreatePoolSessionPersistenceOption().withType(persistenceType);

            if(Objects.equals(persistenceType, CreatePoolSessionPersistenceOption.TypeEnum.APP_COOKIE))
                persistenceOption.withCookieName(input.getCookieName());

            poolOption.withSessionPersistence(
                    persistenceOption
            );
        }

        HuaweiCloudProvider provider = (HuaweiCloudProvider) lbPoolHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        String poolId = provider.buildClient(account).elb().createLbPool(
                new CreatePoolRequest().withBody(
                        new CreatePoolRequestBody().withPool(poolOption)
                )
        );
        resource.setExternalId(poolId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
