package com.stratocloud.provider.tencent.instance.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.image.TencentImageHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentInstanceToImageHandler implements ChangeableEssentialHandler {

    private final TencentInstanceHandler instanceHandler;

    private final TencentImageHandler imageHandler;

    public TencentInstanceToImageHandler(TencentInstanceHandler instanceHandler, TencentImageHandler imageHandler) {
        this.instanceHandler = instanceHandler;
        this.imageHandler = imageHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_INSTANCE_TO_IMAGE_RELATIONSHIP";
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
    public void connect(Relationship relationship) {
        Resource instance = relationship.getSource();
        Resource image = relationship.getTarget();

        ChangeImageInput input = JSON.convert(relationship.getProperties(), ChangeImageInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(instance.getAccountId());

        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        ResetInstanceRequest request = new ResetInstanceRequest();
        request.setInstanceId(instance.getExternalId());

        request.setImageId(image.getExternalId());

        resolveGuestOptions(instance, input, request);

        provider.buildClient(account).resetInstance(request);
    }

    private void resolveGuestOptions(Resource instance, ChangeImageInput input, ResetInstanceRequest request) {
        List<Resource> keyPairs = instance.getRequirementTargets(ResourceCategories.KEY_PAIR);

        boolean keepImageLogin = input.getKeepImageLogin() != null ? input.getKeepImageLogin() : false;

        LoginSettings loginSettings = new LoginSettings();
        loginSettings.setKeepImageLogin(keepImageLogin?"TRUE":null);

        if(Utils.isNotEmpty(keyPairs)){
            loginSettings.setKeyIds(keyPairs.stream().map(Resource::getExternalId).toArray(String[]::new));
        }else {
            loginSettings.setPassword(input.getPassword());
        }

        request.setLoginSettings(loginSettings);

        request.setEnhancedService(getEnhancedServiceOptions(input));

        request.setUserData(input.getUserData());
    }

    private EnhancedService getEnhancedServiceOptions(ChangeImageInput input) {
        RunAutomationServiceEnabled automationServiceEnabled = new RunAutomationServiceEnabled();
        RunSecurityServiceEnabled securityServiceEnabled = new RunSecurityServiceEnabled();
        RunMonitorServiceEnabled monitorService = new RunMonitorServiceEnabled();

        automationServiceEnabled.setEnabled(input.getEnableAutomationService());
        securityServiceEnabled.setEnabled(input.getEnableSecurityService());
        monitorService.setEnabled(input.getEnableMonitorService());


        EnhancedService enhancedService = new EnhancedService();
        enhancedService.setAutomationService(automationServiceEnabled);
        enhancedService.setSecurityService(securityServiceEnabled);
        enhancedService.setMonitorService(monitorService);
        return enhancedService;
    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        if(isConnected(relationship, account))
            return RelationshipActionResult.finished();

        return TencentInstanceUtil.checkLastOperationStateForRelationship(
                instanceHandler, account, relationship.getSource()
        );
    }

    @Override
    public Class<? extends RelationshipConnectInput> getConnectInputClass() {
        return ChangeImageInput.class;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Instance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();


        Optional<ExternalResource> image
                = imageHandler.describeExternalResource(account, instance.get().getImageId());

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
        @BooleanField(label = "保存镜像登录设置")
        private Boolean keepImageLogin;
        @InputField(
                label = "初始密码",
                inputType = "password",
                conditions = "this.keepImageLogin!==true",
                required = false
        )
        private String password;

        @InputField(label = "用户数据", inputType = "textarea")
        private String userData;

        @BooleanField(label = "启用云自动化助手", defaultValue = true)
        private Boolean enableAutomationService;
        @BooleanField(label = "启用云安全服务", defaultValue = true)
        private Boolean enableSecurityService;
        @BooleanField(label = "启用云监控服务", defaultValue = true)
        private Boolean enableMonitorService;
    }
}
