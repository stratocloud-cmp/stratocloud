package com.stratocloud.provider.tencent.cos.lifecycle.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleHandler;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentLifecycleRuleToBucketHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_LIFECYCLE_RULE_TO_BUCKET_RELATIONSHIP";
    private final TencentBucketLifecycleRuleHandler ruleHandler;

    private final TencentBucketHandler bucketHandler;

    public TencentLifecycleRuleToBucketHandler(TencentBucketLifecycleRuleHandler ruleHandler,
                                               TencentBucketHandler bucketHandler) {
        this.ruleHandler = ruleHandler;
        this.bucketHandler = bucketHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云存储桶与生命周期";
    }

    @Override
    public ResourceHandler getSource() {
        return ruleHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return bucketHandler;
    }

    @Override
    public String getCapabilityName() {
        return "生命周期";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        TencentBucketLifecycleRuleId ruleId = TencentBucketLifecycleRuleId.fromString(source.externalId());

        Optional<ExternalResource> bucket = bucketHandler.describeExternalResource(account, ruleId.bucketName());

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
