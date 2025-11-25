package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsUtil;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiDcsResizeHandler implements ResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiDcsResizeHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "变更Redis规格";
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
        return ResizeInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();

        Optional<InstanceListInfo> instance = dcsHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return Optional.empty();

        HuaweiCloudClient client = provider.buildClient(account);

        Optional<FlavorsItems> currentFlavor = client.dcs().describeFlavor(instance.get().getSpecCode());

        if(currentFlavor.isEmpty())
            return Optional.empty();

        ListFlavorsRequest request = new ListFlavorsRequest();

        request.setCacheMode(currentFlavor.get().getCacheMode());
        request.setEngine(currentFlavor.get().getEngine());
        request.setEngineVersion(instance.get().getEngineVersion());
        request.setCpuType(ListFlavorsRequest.CpuTypeEnum.fromValue(currentFlavor.get().getCpuType()));

        List<FlavorsItems> flavors = client.dcs().describeFlavors(request).stream().filter(
                f -> Objects.equals(f.getReplicaCount(), currentFlavor.get().getReplicaCount())
        ).filter(
                f -> Objects.equals(HuaweiDcsUtil.getShardingNum(f), HuaweiDcsUtil.getShardingNum(currentFlavor.get()))
        ).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "specCode",
                flavors.stream().map(FlavorsItems::getSpecCode).toList(),
                flavors.stream().map(HuaweiDcsUtil::getFlavorName).toList()
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        FlavorsItems flavor = client.dcs().describeFlavor(input.getSpecCode()).orElseThrow(
                () -> new StratoException("Flavor not found")
        );

        ResizeInstanceRequest request = new ResizeInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        ResizeInstanceBody body = new ResizeInstanceBody();
        body.setSpecCode(flavor.getSpecCode());
        body.setNewCapacity(Integer.valueOf(flavor.getCapacity().get(0)));
        body.setExecuteImmediately(true);
        request.setBody(body);

        client.dcs().resizeInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> instance = dcsHandler.describeExternalResource(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceActionResult.finished();

        if(instance.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

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
    public static class ResizeInput implements ResourceActionInput {
        @SelectField(
                label = "实例规格"
        )
        private String specCode;
    }
}
