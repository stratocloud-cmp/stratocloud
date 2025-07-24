package com.stratocloud.provider.tencent.cos.acl.actions;

import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Grant;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
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
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TencentBucketAclDestroyHandler implements DestroyResourceActionHandler {

    private final TencentBucketAclHandler aclHandler;

    public TencentBucketAclDestroyHandler(TencentBucketAclHandler aclHandler) {
        this.aclHandler = aclHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return aclHandler;
    }

    @Override
    public String getTaskName() {
        return "删除存储桶ACL";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        TencentBucketAclId aclId = TencentBucketAclId.fromString(resource.getExternalId());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) aclHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        var acl = cosSession.describeBucketAcl(aclId.bucketName());

        if(acl.isEmpty())
            return;

        List<Grant> currentGrants = acl.get().getGrantsAsList();
        if(Utils.isEmpty(currentGrants))
            return;

        List<Grant> grantsToKeep = currentGrants.stream().filter(
                g -> !Objects.equals(aclId.granteeIdentifier(), g.getGrantee().getIdentifier())
        ).toList();

        AccessControlList newAcl = new AccessControlList();
        newAcl.setOwner(acl.get().getOwner());
        for (Grant grant : grantsToKeep) {
            newAcl.grantPermission(grant.getGrantee(), grant.getPermission());
        }

        cosSession.setBucketAcl(aclId.bucketName(), newAcl);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        resource.onDestroyed();
        return ResourceActionResult.finished();
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentBucketAclToBucketHandler.TYPE_ID
        );
    }
}
