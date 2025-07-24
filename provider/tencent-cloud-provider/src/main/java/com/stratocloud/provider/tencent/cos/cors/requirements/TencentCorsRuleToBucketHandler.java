package com.stratocloud.provider.tencent.cos.cors.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleHandler;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentCorsRuleToBucketHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_CORS_RULE_TO_BUCKET_RELATIONSHIP";
    private final TencentBucketCorsRuleHandler ruleHandler;

    private final TencentBucketHandler bucketHandler;

    public TencentCorsRuleToBucketHandler(TencentBucketCorsRuleHandler ruleHandler,
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
        return "腾讯云存储桶与CORS";
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
        return "CORS";
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
        TencentBucketCorsRuleId ruleId = TencentBucketCorsRuleId.fromString(source.externalId());

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
