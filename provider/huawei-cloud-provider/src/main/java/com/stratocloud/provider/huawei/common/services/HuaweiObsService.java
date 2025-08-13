package com.stratocloud.provider.huawei.common.services;

import com.obs.services.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiObsService {
    List<ObsBucket> describeBuckets();

    Optional<ObsBucket> describeBucket(String bucketName);

    List<VersionOrDeleteMarker> describeObjectVersions(String bucketName);

    void deleteVersions(String bucketName, List<KeyAndVersion> keyVersions);

    boolean doesBucketExist(String bucketName);

    Optional<BucketMetadataInfoResult> getBucketInfo(String bucketName);

    ObsBucket createBucket(CreateBucketRequest request);

    void deleteBucket(String bucketName);

    void setBucketStorageClass(String bucketName, StorageClassEnum storageClass);

    Optional<AccessControlList> describeBucketAcl(String bucketName);

    void setBucketAcl(String bucketName, AccessControlList acl);

    Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName);

    void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration);

    Optional<BucketEncryption> describeBucketEncryption(String bucketName);

    void setBucketEncryption(SetBucketEncryptionRequest request);

    void deleteBucketEncryption(String bucketName);

    Optional<BucketLoggingConfiguration> describeBucketLogging(String bucketName);

    void setBucketLogging(String bucketName, String targetBucket, String targetPrefix, String agency);

    void deleteBucketLogging(String bucketName);

    Optional<BucketCors> describeBucketCors(String bucketName);

    void setBucketCors(String bucketName, BucketCors cors);

    void deleteBucketCors(String bucketName);

    Optional<LifecycleConfiguration> describeBucketLifecycle(String bucketName);

    void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration);

    void deleteBucketLifecycle(String bucketName);

    Optional<BucketPolicyResponse> describeBucketPolicy(String bucketName);

    void setBucketPolicy(String bucketName, String policyText);

    void deleteBucketPolicy(String bucketName);

    Optional<WebsiteConfiguration> describeBucketWebsite(String bucketName);

    void setBucketWebsite(SetBucketWebsiteRequest request);

    void deleteBucketWebsite(String bucketName);


    Optional<BucketStorageInfo> describeBucketStat(String bucketName);
}
