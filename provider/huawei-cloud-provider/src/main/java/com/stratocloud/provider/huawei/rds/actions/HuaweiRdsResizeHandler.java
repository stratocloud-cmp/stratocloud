package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.huawei.rds.HuaweiRdsUtil;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiRdsResizeHandler implements ResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsResizeHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "RDS实例变更配置";
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

        Optional<InstanceResponse> rdsInstance = rdsHandler.describeInstance(account, resource.getExternalId());

        if(rdsInstance.isEmpty())
            return Optional.empty();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);
        Optional<Flavor> flavor = client.rds().describeFlavor(
                ListFlavorsRequest.DatabaseNameEnum.fromValue(
                        rdsInstance.get().getDatastore().getType().getValue()
                ),
                rdsInstance.get().getFlavorRef()
        );
        ListFlavorsResizeResponse response = client.rds().describeResizeTargetFlavors(resource.getExternalId());

        List<ComputeFlavorGroup> flavorGroups = response.getFlavorGroups();

        List<String> groupTypes = new ArrayList<>();
        List<String> groupTypeNames = new ArrayList<>();

        List<String> flavorCodes = new ArrayList<>();
        List<String> flavorNames = new ArrayList<>();
        List<String> groupTypePropertyValues = new ArrayList<>();

        if(Utils.isNotEmpty(flavorGroups)){
            for (ComputeFlavorGroup flavorGroup : flavorGroups) {
                groupTypes.add(flavorGroup.getGroupType());
                groupTypeNames.add(
                        HuaweiRdsBuildInput.EngineInput.getFlavorGroupName(flavorGroup.getGroupType())
                );

                if(Utils.isNotEmpty(flavorGroup.getComputeFlavors())){
                    for (ComputeFlavor computeFlavor : flavorGroup.getComputeFlavors()) {
                        flavorCodes.add(computeFlavor.getCode());
                        flavorNames.add(getFlavorName(computeFlavor));
                        groupTypePropertyValues.add(flavorGroup.getGroupType());
                    }
                }
            }
        }

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "groupType",
                groupTypes,
                groupTypeNames
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "flavorCode",
                flavorCodes,
                flavorNames
        );

        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "flavorCode",
                "groupType",
                "规格类型",
                groupTypePropertyValues,
                false
        );


        ResizeInput resizeInput = new ResizeInput();
        resizeInput.setResizeFlavor(true);
        resizeInput.setVolumeSize(rdsInstance.get().getVolume().getSize());
        if(flavor.isPresent()) {
            resizeInput.setFlavorCode(flavor.get().getSpecCode());
            resizeInput.setGroupType(flavor.get().getGroupType());
        }



        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, resizeInput);

        return Optional.of(formMetaData);
    }

    private static String getFlavorName(ComputeFlavor flavor){
        return "%s %sC%sG".formatted(
                flavor.getCode(),
                flavor.getVcpus(),
                flavor.getRam()
        );
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        InstanceResponse rdsInstance = rdsHandler.describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("RDS instance not found")
        );

        if(input.isResizeFlavor()){
            StartResizeFlavorActionRequest request = new StartResizeFlavorActionRequest();
            request.setInstanceId(resource.getExternalId());
            request.setBody(new ResizeFlavorRequest().withResizeFlavor(
                    new ResizeFlavorObject().withIsAutoPay(true).withIsDelay(false).withSpecCode(input.getFlavorCode())
            ));

            String jobId = client.rds().resizeInstance(request);

            HuaweiRdsUtil.waitForJob(client, jobId);
        }

        if(input.isEnlargeVolume() && input.getVolumeSize() > rdsInstance.getVolume().getSize()){
            StartInstanceEnlargeVolumeActionRequest request = new StartInstanceEnlargeVolumeActionRequest();
            request.setInstanceId(resource.getExternalId());
            request.setBody(
                    new EnlargeVolumeRequestBody().withEnlargeVolume(
                            new EnlargeVolumeObject().withSize(input.getVolumeSize()).withIsAutoPay(true)
                    )
            );

            String jobId = client.rds().enlargeVolume(request);
            TaskContext.setExternalTaskId(jobId);
        }
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return HuaweiRdsUtil.checkActionResult(resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceResponse rdsInstance = rdsHandler.describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("RDS instance not found")
        );

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        if(input.isEnlargeVolume() && input.getVolumeSize() < rdsInstance.getVolume().getSize())
            throw new BadCommandException("扩容后的硬盘大小不能小于扩容之前");
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @BooleanField(label = "是否变更规格", defaultValue = true)
        private boolean resizeFlavor;
        @SelectField(label = "规格类型", conditions = "this.resizeFlavor === true")
        private String groupType;
        @SelectField(
                label = "规格",
                filterPredicates = "!formData.groupType || element.groupType === formData.groupType",
                conditions = "this.resizeFlavor === true"
        )
        private String flavorCode;
        @BooleanField(label = "是否扩容硬盘")
        private boolean enlargeVolume;
        @NumberField(label = "硬盘大小(GB)",min = 40, max = 4000, step = 10, conditions = "this.enlargeVolume === true")
        private Integer volumeSize;
    }
}
