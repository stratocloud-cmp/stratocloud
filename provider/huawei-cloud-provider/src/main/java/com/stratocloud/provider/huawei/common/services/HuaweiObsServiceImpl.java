package com.stratocloud.provider.huawei.common.services;

import com.obs.services.IObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class HuaweiObsServiceImpl implements HuaweiObsService {

    private final String accessKey;
    private final String secretKey;
    private final String regionId;

    public HuaweiObsServiceImpl(String accessKey, String secretKey, String regionId) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.regionId = regionId;
    }

    private IObsClient buildClient(){
        return HuaweiObsSessionManager.getSession(
                new HuaweiObsSessionManager.ObsSessionKey(
                        regionId,
                        accessKey,
                        secretKey
                )
        );
    }

    public static void tryRunnable(Runnable runnable){
        Supplier<Boolean> supplier = () -> {
            runnable.run();
            return true;
        };
        trySupplier(supplier);
    }

    public static <R> R trySupplier(Supplier<R> supplier){
        return doTrySupplier(supplier, 0);
    }

    public static <R> R doTrySupplier(Supplier<R> supplier, int triedTimes) {
        if(triedTimes >= 5)
            throw new StratoException("Max triedTimes exceeded: "+triedTimes);

        try {
            return supplier.get();
        }catch (ObsException e){
            String errorCode = e.getErrorCode();
            log.warn("ObsErrorCode: {}", errorCode);

            if(Utils.isBlank(errorCode))
                throw new StratoException(e.getMessage(), e);

            Set<String> retryingErrorCodes = Set.of(
                    "TooManyRequests",
                    "ServiceUnavailable",
                    "SlowDown"
            );

            if(retryingErrorCodes.contains(errorCode)){
                log.warn("Retrying obs request later: {}", e.getMessage());
                SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                return doTrySupplier(supplier, triedTimes+1);
            }

            if(errorCode.startsWith("NoSuch"))
                throw new ExternalResourceNotFoundException(e.getMessage(), e);

            throw new StratoException(e.getMessage(), e);
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private <R> Optional<R> queryObsOne(Supplier<R> supplier){
        try {
            return Optional.ofNullable(trySupplier(supplier));
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }


    private <R, E> List<E> queryObsAll(Supplier<R> supplier,
                                       Function<R, List<E>> listGetter,
                                       Function<R, String> nextMarkerGetter,
                                       Consumer<String> markerSetter){

        List<E> result = new ArrayList<>();
        try {
            R r = trySupplier(supplier);

            List<E> page = listGetter.apply(r);
            if(Utils.isNotEmpty(page))
                result.addAll(page);

            String nextMarker = nextMarkerGetter.apply(r);

            while (Utils.isNotBlank(nextMarker)){
                markerSetter.accept(nextMarker);

                r = trySupplier(supplier);

                page = listGetter.apply(r);
                if(Utils.isNotEmpty(page))
                    result.addAll(page);
            }

            return result;
        } catch (ExternalResourceNotFoundException e) {
            return result;
        }
    }



    @Override
    public List<ObsBucket> describeBuckets(){
        ListBucketsRequest request = new ListBucketsRequest();
        return queryObsAll(
                () -> buildClient().listBucketsV2(request),
                ListBucketsResult::getBuckets,
                ListBucketsResult::getNextMarker,
                request::setMarker
        ).stream().filter(
                b -> Objects.equals(regionId, b.getLocation())
        ).toList();
    }

    @Override
    public Optional<ObsBucket> describeBucket(String bucketName){
        return describeBuckets().stream().filter(
                b -> Objects.equals(b.getBucketName(), bucketName)
        ).findAny();
    }

    @Override
    public List<VersionOrDeleteMarker> describeObjectVersions(String bucketName){
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        return queryObsAll(
                () -> buildClient().listVersions(request),
                result -> List.of(result.getVersions()),
                ListVersionsResult::getNextKeyMarker,
                request::setKeyMarker
        );
    }

    @Override
    public void deleteVersions(String bucketName, List<KeyAndVersion> keyVersions){
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setKeyAndVersions(keyVersions.toArray(KeyAndVersion[]::new));
        tryRunnable(() -> buildClient().deleteObjects(request));
        log.info("Huawei bucket objects deleted. BucketName={}. Count={}. KeyVersions={}.",
                bucketName, keyVersions.size(), JSON.toJsonString(keyVersions));
    }

    @Override
    public boolean doesBucketExist(String bucketName){
        try {
            buildClient().getBucketAcl(bucketName);
        }catch (ObsException e){
            if(e.getResponseCode() == 404)
                return false;
        }
        return true;
    }

    @Override
    public Optional<BucketMetadataInfoResult> getBucketInfo(String bucketName){
        return queryObsOne(() -> buildClient().getBucketMetadata(new BucketMetadataInfoRequest(bucketName)));
    }

    @Override
    public ObsBucket createBucket(CreateBucketRequest request){
        ObsBucket bucket = trySupplier(
                () -> buildClient().createBucket(request)
        );
        log.info("Huawei obs bucket created. Bucket={}.", bucket.getBucketName());
        return bucket;
    }

    @Override
    public void deleteBucket(String bucketName){
        if(doesBucketExist(bucketName)){
            tryRunnable(() -> buildClient().deleteBucket(bucketName));
            log.info("Huawei obs bucket deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public void setBucketStorageClass(String bucketName, StorageClassEnum storageClass){
        tryRunnable(
                () -> buildClient().setBucketStoragePolicy(
                        bucketName,
                        new BucketStoragePolicyConfiguration(storageClass)
                )
        );
        log.info("Huawei obs bucket storage class set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<AccessControlList> describeBucketAcl(String bucketName){
        return queryObsOne(() -> buildClient().getBucketAcl(bucketName));
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl){
        tryRunnable(() -> buildClient().setBucketAcl(bucketName, acl));
        log.info("Huawei obs bucket acl set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryObsOne(
                    () -> buildClient().getBucketVersioning(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration){
        tryRunnable(
                () -> buildClient().setBucketVersioning(bucketName, configuration)
        );
        log.info("Huawei obs bucket versioning set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketEncryption> describeBucketEncryption(String bucketName) {
        return queryObsOne(() -> buildClient().getBucketEncryption(bucketName));
    }

    @Override
    public void setBucketEncryption(SetBucketEncryptionRequest request){
        tryRunnable(() -> buildClient().setBucketEncryption(request));
        log.info("Huawei obs bucket encryption set. Bucket={}.", request.getBucketName());
    }

    @Override
    public void deleteBucketEncryption(String bucketName){
        Optional<BucketEncryption> encryption = describeBucketEncryption(bucketName);

        if(encryption.isPresent()){
            tryRunnable(() -> buildClient().deleteBucketEncryption(bucketName));
            log.info("Huawei obs bucket encryption deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketLoggingConfiguration> describeBucketLogging(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryObsOne(
                    () -> buildClient().getBucketLogging(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketLogging(String bucketName, String targetBucket, String targetPrefix, String agency){
        BucketLoggingConfiguration configuration = new BucketLoggingConfiguration(targetBucket, targetPrefix);
        configuration.setAgency(agency);
        tryRunnable(
                () -> buildClient().setBucketLogging(
                        new SetBucketLoggingRequest(
                                bucketName,
                                configuration
                        )
                )
        );
        log.info("Huawei obs bucket logging set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketLogging(String bucketName){
        Optional<BucketLoggingConfiguration> logging = describeBucketLogging(bucketName);
        if(logging.isPresent()){
            tryRunnable(() -> buildClient().setBucketLogging(bucketName, new BucketLoggingConfiguration()));
            log.info("Huawei obs bucket logging deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketCors> describeBucketCors(String bucketName){
        return queryObsOne(() -> buildClient().getBucketCors(bucketName));
    }

    @Override
    public void setBucketCors(String bucketName, BucketCors cors){
        tryRunnable(
                () -> buildClient().setBucketCors(bucketName, cors)
        );
        log.info("Huawei obs bucket cors set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketCors(String bucketName){
        Optional<BucketCors> configuration = describeBucketCors(bucketName);

        if(configuration.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketCors(bucketName));
            log.info("Huawei obs bucket cors deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<LifecycleConfiguration> describeBucketLifecycle(String bucketName){
        return queryObsOne(() -> buildClient().getBucketLifecycle(bucketName));
    }

    @Override
    public void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration){
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName, lifecycleConfiguration);
        tryRunnable(() -> buildClient().setBucketLifecycle(request));
        log.info("Huawei obs bucket lifecycle set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketLifecycle(String bucketName){
        Optional<LifecycleConfiguration> configuration = describeBucketLifecycle(bucketName);

        if(configuration.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketLifecycle(bucketName));
            log.info("Huawei obs bucket lifecycle deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketPolicyResponse> describeBucketPolicy(String bucketName){
        return queryObsOne(
                () -> buildClient().getBucketPolicyV2(bucketName)
        );
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText){
        tryRunnable(() -> buildClient().setBucketPolicy(bucketName, policyText));
        log.info("Huawei obs bucket policy set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketPolicy(String bucketName){
        Optional<BucketPolicyResponse> policy = describeBucketPolicy(bucketName);

        if(policy.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketPolicy(bucketName));
            log.info("Huawei obs bucket policy deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<WebsiteConfiguration> describeBucketWebsite(String bucketName){
        return queryObsOne(
                () -> buildClient().getBucketWebsite(bucketName)
        );
    }

    @Override
    public void setBucketWebsite(SetBucketWebsiteRequest request){
        tryRunnable(() -> buildClient().setBucketWebsite(request));
        log.info("Huawei obs bucket website set. Bucket={}.", request.getBucketName());
    }

    @Override
    public void deleteBucketWebsite(String bucketName){
        var website = describeBucketWebsite(bucketName);

        if(website.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketWebsite(bucketName));
            log.info("Huawei obs bucket website deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketStorageInfo> describeBucketStat(String bucketName){
        return queryObsOne(() -> buildClient().getBucketStorageInfo(bucketName));
    }
}
