package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.oss.model.*;

import java.util.List;
import java.util.Optional;

public interface AliyunOssService {
    List<Bucket> describeBuckets();

    Optional<Bucket> describeBucket(String bucketName);

    List<OSSVersionSummary> describeObjectVersions(String bucketName);

    void deleteVersions(String bucketName, List<DeleteVersionsRequest.KeyVersion> keyVersions);

    boolean doesBucketExist(String bucketName);

    Optional<BucketInfo> getBucketInfo(String bucketName);

    Bucket createBucket(CreateBucketRequest request);

    void deleteBucket(String bucketName);

    void setBucketAcl(String bucketName, CannedAccessControlList acl);

    Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName);

    void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration);

    Optional<ServerSideEncryptionConfiguration> describeBucketEncryption(String bucketName);

    void setBucketEncryption(SetBucketEncryptionRequest request);

    void deleteBucketEncryption(String bucketName);

    Optional<BucketLoggingResult> describeBucketLogging(String bucketName);

    void setBucketLogging(String bucketName, String targetBucket, String targetPrefix);

    void deleteBucketLogging(String bucketName);

    Optional<CORSConfiguration> describeBucketCors(String bucketName);

    void setBucketCors(String bucketName, List<SetBucketCORSRequest.CORSRule> rules);

    void deleteBucketCors(String bucketName);

    List<LifecycleRule> describeBucketLifecycle(String bucketName);

    void setBucketLifecycle(String bucketName, List<LifecycleRule> rules);

    void deleteBucketLifecycle(String bucketName);

    Optional<GetBucketPolicyResult> describeBucketPolicy(String bucketName);

    void setBucketPolicy(String bucketName, String policyText);

    void deleteBucketPolicy(String bucketName);

    Optional<BucketReferer> describeBucketReferer(String bucketName);

    void setBucketReferer(String bucketName, BucketReferer referer);

    Optional<BucketWebsiteResult> describeBucketWebsite(String bucketName);

    void setBucketWebsite(SetBucketWebsiteRequest request);

    void deleteBucketWebsite(String bucketName);

    Optional<AccessMonitor> describeAccessMonitor(String bucketName);

    void setAccessMonitor(String bucketName, boolean enabled);

    Optional<BucketStat> describeBucketStat(String bucketName);
}
