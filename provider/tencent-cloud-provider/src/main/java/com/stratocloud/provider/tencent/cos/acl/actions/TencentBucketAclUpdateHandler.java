package com.stratocloud.provider.tencent.cos.acl.actions;

import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Permission;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.BooleanFieldDetail;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.acl.TencentBucketAcl;
import com.stratocloud.provider.tencent.cos.acl.TencentBucketAclHandler;
import com.stratocloud.provider.tencent.cos.acl.TencentBucketAclId;
import com.stratocloud.provider.tencent.cos.acl.requirements.TencentBucketAclToBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentBucketAclUpdateHandler implements BuildResourceActionHandler {

    private final TencentBucketAclHandler aclHandler;

    public TencentBucketAclUpdateHandler(TencentBucketAclHandler aclHandler) {
        this.aclHandler = aclHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return aclHandler;
    }

    @Override
    public String getTaskName() {
        return "更新存储桶ACL";
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<TencentBucketAcl> bucketAcl = aclHandler.describeBucketAcl(account, resource.getExternalId());

        if(bucketAcl.isEmpty())
            return Optional.empty();

        Set<Permission> permissions = bucketAcl.get().permissions();

        if(permissions == null)
            permissions = new HashSet<>();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketAclUpdateInput.class);

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "allowFullControl",
                new BooleanFieldDetail(
                        permissions.contains(Permission.FullControl),
                        List.of()
                )
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "allowRead",
                new BooleanFieldDetail(
                        permissions.contains(Permission.Read),
                        List.of("this.allowFullControl === false")
                )
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "allowWrite",
                new BooleanFieldDetail(
                        permissions.contains(Permission.Write),
                        List.of("this.allowFullControl === false")
                )
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "allowReadAcp",
                new BooleanFieldDetail(
                        permissions.contains(Permission.ReadAcp),
                        List.of("this.allowFullControl === false")
                )
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "allowWriteAcp",
                new BooleanFieldDetail(
                        permissions.contains(Permission.WriteAcp),
                        List.of("this.allowFullControl === false")
                )
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketAclUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketAclUpdateInput input = JSON.convert(parameters, TencentBucketAclUpdateInput.class);

        TencentBucketAclId aclId = TencentBucketAclId.fromString(resource.getExternalId());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) aclHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        AccessControlList acl = cosSession.describeBucketAcl(
                aclId.bucketName()
        ).orElseThrow(
                () -> new StratoException("Bucket acl not found")
        );

        AccessControlList newAcl = input.toSpec(aclId.granteeIdentifier()).apply(acl);
        cosSession.setBucketAcl(aclId.bucketName(), newAcl);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        TencentBucketAclUpdateInput input = JSON.convert(parameters, TencentBucketAclUpdateInput.class);

        boolean noPermissionAllowed = !input.isAllowFullControl() && !input.isAllowRead() &&
                !input.isAllowWrite() && !input.isAllowReadAcp() && !input.isAllowWriteAcp();

        if(noPermissionAllowed)
            throw new BadCommandException("当前配置无意义，请至少允许一项权限");
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentBucketAclToBucketHandler.TYPE_ID
        );
    }
}
