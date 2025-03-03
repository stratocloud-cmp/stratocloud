package com.stratocloud.provider.aliyun.instance.requirements;

import com.aliyun.ecs20140526.models.ReplaceSystemDiskRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.image.AliyunImageHandler;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AliyunInstanceToImageHandler implements ChangeableEssentialHandler {

    private final AliyunInstanceHandler instanceHandler;

    private final AliyunImageHandler imageHandler;

    public AliyunInstanceToImageHandler(AliyunInstanceHandler instanceHandler, AliyunImageHandler imageHandler) {
        this.instanceHandler = instanceHandler;
        this.imageHandler = imageHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_INSTANCE_TO_IMAGE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与镜像";
    }

    @Override
    public ResourceHandler getSource() {
        return instanceHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return imageHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机";
    }

    @Override
    public String getRequirementName() {
        return "镜像";
    }

    @Override
    public String getConnectActionName() {
        return "应用镜像";
    }

    @Override
    public String getDisconnectActionName() {
        return "弃用镜像";
    }

    @Override
    public Set<ResourceState> getAllowedSourceStates() {
        return Set.of(ResourceState.STOPPED);
    }

    @Override
    public void connect(Relationship relationship) {
        Resource instance = relationship.getSource();
        Resource image = relationship.getTarget();

        if(instance.getState() != ResourceState.STOPPED)
            throw new BadCommandException("请先关机再执行变更镜像");

        ChangeImageInput input = JSON.convert(relationship.getProperties(), ChangeImageInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());

        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        ReplaceSystemDiskRequest request = new ReplaceSystemDiskRequest();


        request.setImageId(image.getExternalId());
        request.setInstanceId(instance.getExternalId());

        request.setEncrypted(input.getEncrypted());

        request.setInstanceId(instance.getExternalId());
        request.setImageId(image.getExternalId());


        resolveGuestOptions(instance, input, request);

        client.ecs().replaceSystemDisk(request);
    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        var result = ChangeableEssentialHandler.super.checkConnectResult(account, relationship);

        Resource instance = relationship.getSource();
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        Optional<AliyunInstance> aliyunInstance = instanceHandler.describeInstance(account, instance.getExternalId());

        if(aliyunInstance.isEmpty())
            return result;

        Optional<AliyunDisk> systemDisk = provider.buildClient(account).ecs().describeSystemDiskByInstanceId(
                aliyunInstance.get().detail().getInstanceId()
        );

        Optional<Resource> systemDiskResource = instance.getPrimaryCapability(ResourceCategories.DISK);

        if(systemDisk.isEmpty() || systemDiskResource.isEmpty())
            return result;

        String oldSystemDiskId = systemDiskResource.get().getExternalId();
        String newSystemDiskId = systemDisk.get().detail().getDiskId();

        if(Objects.equals(oldSystemDiskId, newSystemDiskId))
            return result;

        log.info("System disk id changed from {} to {} due to action: ReplaceSystemDisk.",
                oldSystemDiskId, newSystemDiskId);

        systemDiskResource.get().setExternalId(newSystemDiskId);

        systemDiskResource.get().synchronize();

        if(systemDiskResource.get().getState() == ResourceState.ATTACHING){
            log.info("New system disk {} is attaching, checking later...", newSystemDiskId);
            return RelationshipActionResult.inProgress();
        }

        return result;
    }

    private void resolveGuestOptions(Resource instance, ChangeImageInput input, ReplaceSystemDiskRequest request) {
        Optional<Resource> keyPair = instance.getExclusiveTarget(ResourceCategories.KEY_PAIR);

        boolean passwordInherit = input.getPasswordInherit() != null ? input.getPasswordInherit() : false;

        request.setPasswordInherit(passwordInherit);

        if(keyPair.isPresent()){
            request.setKeyPairName(keyPair.get().getName());
        }else if(!passwordInherit) {
            request.setPassword(input.getPassword());
        }

        if(Utils.isNotBlank(input.getSecurityEnhancementStrategy()))
            request.setSecurityEnhancementStrategy(input.getSecurityEnhancementStrategy());
    }

    @Override
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return ChangeImageInput.class;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();


        Optional<ExternalResource> image
                = imageHandler.describeExternalResource(account, instance.get().detail().getImageId());

        if(image.isEmpty())
            return List.of();

        ExternalRequirement imageRequirement = new ExternalRequirement(
                getRelationshipTypeId(),
                image.get(),
                Map.of()
        );

        return List.of(imageRequirement);
    }




    @Data
    public static class ChangeImageInput implements RelationshipConnectInput{
        @BooleanField(label = "保存镜像登录密码")
        private Boolean passwordInherit;
        @InputField(
                label = "初始密码",
                inputType = "password",
                conditions = "this.passwordInherit!==true",
                required = false
        )
        private String password;


        @BooleanField(label = "加密云硬盘")
        private Boolean encrypted;

        @SelectField(
                label = "云安全中心服务",
                options = {"Active", "Deactive"},
                optionNames = {"启用", "不启用"},
                defaultValues = "Deactive"
        )
        private String securityEnhancementStrategy;
    }
}
