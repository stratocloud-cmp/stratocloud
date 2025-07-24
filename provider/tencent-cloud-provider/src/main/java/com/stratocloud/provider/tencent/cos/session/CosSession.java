package com.stratocloud.provider.tencent.cos.session;

import com.qcloud.cos.model.*;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRule;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleId;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRule;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleId;

import java.util.List;
import java.util.Optional;

public interface CosSession {
    List<Bucket> describeBuckets();

    Optional<Bucket> describeBucket(String bucketName);

    List<COSVersionSummary> describeObjectVersions(String bucketName);

    void deleteObjects(String bucketName, List<DeleteObjectsRequest.KeyVersion> keyVersions);

    boolean doesBucketExist(String bucketName);

    Optional<HeadBucketResult> headBucket(String bucketName);

    Bucket createBucket(CreateBucketRequest request, boolean multiAz);

    void deleteBucket(String bucketName);

    Optional<AccessControlList> describeBucketAcl(String bucketName);

    void setBucketAcl(String bucketName, AccessControlList accessControlList);

    Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName);

    void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration);

    Optional<BucketLoggingConfiguration> describeBucketLogging(String bucketName);

    void setBucketLogging(String bucketName, BucketLoggingConfiguration configuration);

    Optional<BucketIntelligentTierConfiguration> describeDefaultBucketIntelligentTier(String bucketName);

    void setDefaultBucketIntelligentTier(String bucketName, BucketIntelligentTierConfiguration configuration);


    List<TencentBucketCorsRule> describeBucketCorsRules();

    Optional<TencentBucketCorsRule> describeBucketCorsRule(TencentBucketCorsRuleId ruleId);

    List<TencentBucketCorsRule> describeBucketCorsRulesByBucket(String bucketName);

    void setBucketCors(String bucketName, BucketCrossOriginConfiguration configuration);

    void deleteBucketCors(String bucketName);

    List<TencentBucketLifecycleRule> describeBucketLifecycleRules();

    Optional<TencentBucketLifecycleRule> describeBucketLifecycleRule(TencentBucketLifecycleRuleId ruleId);

    List<TencentBucketLifecycleRule> describeBucketLifecycleRulesByBucket(String bucketName);

    void setBucketLifecycle(String bucketName, BucketLifecycleConfiguration configuration);

    void deleteBucketLifecycle(String bucketName);

    Optional<BucketPolicy> describeBucketPolicy(String bucketName);

    void setBucketPolicy(String bucketName, String policyText);

    void deleteBucketPolicy(String bucketName);

    Optional<BucketRefererConfiguration> describeBucketReferer(String bucketName);

    void setBucketReferer(String bucketName, BucketRefererConfiguration configuration);

    Optional<BucketWebsiteConfiguration> describeBucketWebsite(String bucketName);

    void setBucketWebsite(String bucketName, BucketWebsiteConfiguration configuration);

    void deleteBucketWebsite(String bucketName);

    void shutdown();
}
