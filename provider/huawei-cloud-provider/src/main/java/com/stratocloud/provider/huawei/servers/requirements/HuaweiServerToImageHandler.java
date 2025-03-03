package com.stratocloud.provider.huawei.servers.requirements;

import com.huaweicloud.sdk.ecs.v2.model.ServerBlockDevice;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.InputField;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.image.HuaweiImageHandler;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.RandomUtil;
import com.stratocloud.utils.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiServerToImageHandler implements ChangeableEssentialHandler {

    private final HuaweiServerHandler serverHandler;

    private final HuaweiImageHandler imageHandler;

    public HuaweiServerToImageHandler(HuaweiServerHandler serverHandler,
                                      HuaweiImageHandler imageHandler) {
        this.serverHandler = serverHandler;
        this.imageHandler = imageHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SERVER_TO_IMAGE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与镜像";
    }

    @Override
    public ResourceHandler getSource() {
        return serverHandler;
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
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return ChangeImageInput.class;
    }

    @Override
    public void connect(Relationship relationship) {
        Resource server = relationship.getSource();
        Resource image = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(server.getAccountId());
        ServerDetail serverDetail = serverHandler.describeServer(account, server.getExternalId()).orElseThrow(
                () -> new StratoException("Server not found when rebuilding")
        );
        ImageInfo targetImage = imageHandler.describeImage(account, image.getExternalId()).orElseThrow(
                () -> new StratoException("Target image not found when rebuilding")
        );

        ChangeImageInput input = JSON.convert(relationship.getProperties(), ChangeImageInput.class);



        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        HuaweiCloudClient client = provider.buildClient(account);

        String adminPass;

        if(Utils.isNotBlank(input.getAdminPass()))
            adminPass = input.getAdminPass();
        else
            adminPass = RandomUtil.generatePasswordLen12();


        client.ecs().rebuildServer(serverDetail, targetImage, adminPass, input.getUserData());
    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        Resource serverResource = relationship.getSource();
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        Optional<ServerDetail> server = serverHandler.describeServer(account, serverResource.getExternalId());

        if(server.isEmpty())
            return RelationshipActionResult.failed("Server not found.");

        if("REBUILD".equals(server.get().getStatus())){
            log.info("Server {} is in REBUILD status, checking later...", server.get().getName());
            return RelationshipActionResult.inProgress();
        }

        Optional<Resource> systemDiskResource = serverResource.getPrimaryCapability(ResourceCategories.DISK);


        Optional<ServerBlockDevice> systemDisk = provider.buildClient(account).ecs().describeServerSystemDisk(
                server.get().getId()
        );

        if(systemDiskResource.isEmpty() || systemDisk.isEmpty())
            return ChangeableEssentialHandler.super.checkConnectResult(account, relationship);


        String oldSystemDiskId = systemDiskResource.get().getExternalId();
        String newSystemDiskId = systemDisk.get().getId();

        if(Objects.equals(oldSystemDiskId, newSystemDiskId)) {
            var result = ChangeableEssentialHandler.super.checkConnectResult(account, relationship);
            if(result.taskState() != TaskState.FINISHED){
                log.warn("System disk id of server {} did not change, REBUILD action may have failed.",
                        server.get().getName());
            }
            return result;
        }

        log.info("System disk id changed from {} to {} due to action: REBUILD.",
                oldSystemDiskId, newSystemDiskId);

        systemDiskResource.get().setExternalId(newSystemDiskId);

        systemDiskResource.get().synchronize();

        if(systemDiskResource.get().getState() == ResourceState.ATTACHING){
            log.info("New system disk {} is attaching, checking later...", newSystemDiskId);
            return RelationshipActionResult.inProgress();
        }

        return ChangeableEssentialHandler.super.checkConnectResult(account, relationship);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<ServerDetail> server = serverHandler.describeServer(account, source.externalId());

        if(server.isEmpty())
            return List.of();

        Optional<ExternalResource> image
                = imageHandler.describeExternalResource(account, server.get().getImage().getId());

        return image.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }


    @Data
    public static class ChangeImageInput implements RelationshipConnectInput {
        @InputField(
                label = "初始登录密码",
                inputType = "password"
        )
        private String adminPass;

        @InputField(
                label = "自定义数据(user_data)",
                inputType = "textarea",
                required = false
        )
        private String userData;
    }
}
