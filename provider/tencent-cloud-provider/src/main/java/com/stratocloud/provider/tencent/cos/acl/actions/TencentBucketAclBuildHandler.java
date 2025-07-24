package com.stratocloud.provider.tencent.cos.acl.actions;

import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.UinGrantee;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
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

import java.util.List;
import java.util.Map;

@Component
public class TencentBucketAclBuildHandler implements BuildResourceActionHandler {

    private final TencentBucketAclHandler aclHandler;

    public TencentBucketAclBuildHandler(TencentBucketAclHandler aclHandler) {
        this.aclHandler = aclHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return aclHandler;
    }

    @Override
    public String getTaskName() {
        return "创建存储桶ACL";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketAclBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketAclBuildInput input = JSON.convert(parameters, TencentBucketAclBuildInput.class);

        Resource bucketResource = resource.getEssentialTarget(ResourceCategories.BUCKET).orElseThrow(
                () -> new StratoException("Bucket resource not provided")
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) aclHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        AccessControlList acl = cosSession.describeBucketAcl(
                bucketResource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Bucket acl not found")
        );

        AccessControlList newAcl = input.toSpec().apply(acl);
        cosSession.setBucketAcl(bucketResource.getExternalId(), newAcl);

        resource.setExternalId(
                TencentBucketAclId.fromGrantee(
                        bucketResource.getExternalId(),
                        new UinGrantee(input.getGranteeId())
                ).toString()
        );
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        TencentBucketAclBuildInput input = JSON.convert(parameters, TencentBucketAclBuildInput.class);

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
