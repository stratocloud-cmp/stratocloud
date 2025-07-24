package com.stratocloud.provider.tencent.cos.acl.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.cos.acl.TencentBucketAclHandler;
import com.stratocloud.provider.tencent.cos.acl.TencentBucketAclId;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentBucketAclToBucketHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_BUCKET_ACL_TO_BUCKET_RELATIONSHIP";
    private final TencentBucketAclHandler aclHandler;

    private final TencentBucketHandler bucketHandler;

    public TencentBucketAclToBucketHandler(TencentBucketAclHandler aclHandler,
                                           TencentBucketHandler bucketHandler) {
        this.aclHandler = aclHandler;
        this.bucketHandler = bucketHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云存储桶与ACL";
    }

    @Override
    public ResourceHandler getSource() {
        return aclHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return bucketHandler;
    }

    @Override
    public String getCapabilityName() {
        return "ACL";
    }

    @Override
    public String getRequirementName() {
        return "存储桶";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        TencentBucketAclId aclId = TencentBucketAclId.fromString(source.externalId());

        Optional<ExternalResource> bucket = bucketHandler.describeExternalResource(account, aclId.bucketName());

        return bucket.map(
                b -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                b,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
