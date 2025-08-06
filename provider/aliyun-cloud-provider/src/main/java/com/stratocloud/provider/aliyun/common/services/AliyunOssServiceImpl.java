package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.oss.OSS;
import com.aliyun.oss.ServiceException;
import com.aliyun.oss.model.*;
import com.aliyun.teaopenapi.models.Config;
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
public class AliyunOssServiceImpl implements AliyunOssService {

    private final Config config;

    public AliyunOssServiceImpl(Config config) {
        this.config = config;
    }

    private String getOssRegionId(){
        return "oss-%s".formatted(config.getRegionId());
    }

    private OSS buildClient(){
        return AliyunOssSessionManager.getSession(
                new AliyunOssSessionManager.OssSessionKey(
                        config.getRegionId(),
                        config.getAccessKeyId(),
                        config.getAccessKeySecret()
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
        }catch (ServiceException e){
            String errorCode = e.getErrorCode();
            log.warn("OssErrorCode: {}", errorCode);

            if(Utils.isBlank(errorCode))
                throw new StratoException(e.getMessage(), e);

            Set<String> retryingErrorCodes = Set.of(
                    "MetaOperationQpsLimitExceeded",
                    "TotalQpsLimitExceeded",
                    "ActiveRequestLimitExceeded",
                    "QpsLimitExceeded"
            );

            if(retryingErrorCodes.contains(errorCode)){
                log.warn("Retrying oss request later: {}", e.getMessage());
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

    private <R> Optional<R> queryOssOne(Supplier<R> supplier){
        try {
            return Optional.ofNullable(trySupplier(supplier));
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }


    private <R, E> List<E> queryOssAll(Supplier<R> supplier,
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
    public List<Bucket> describeBuckets(){
        return trySupplier(
                () -> buildClient().listBuckets()
        ).stream().filter(
                b -> Objects.equals(getOssRegionId(), b.getLocation())
        ).toList();
    }

    @Override
    public Optional<Bucket> describeBucket(String bucketName){
        return describeBuckets().stream().filter(
                b -> Objects.equals(b.getName(), bucketName)
        ).findAny();
    }

    @Override
    public List<OSSVersionSummary> describeObjectVersions(String bucketName){
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        return queryOssAll(
                () -> buildClient().listVersions(request),
                VersionListing::getVersionSummaries,
                VersionListing::getNextKeyMarker,
                request::setKeyMarker
        );
    }

    @Override
    public void deleteVersions(String bucketName, List<DeleteVersionsRequest.KeyVersion> keyVersions){
        DeleteVersionsRequest request = new DeleteVersionsRequest(bucketName);
        request.setKeys(keyVersions);
        tryRunnable(() -> buildClient().deleteVersions(request));
        log.info("Aliyun bucket objects deleted. BucketName={}. Count={}. KeyVersions={}.",
                bucketName, keyVersions.size(), JSON.toJsonString(keyVersions));
    }

    @Override
    public boolean doesBucketExist(String bucketName){
        return trySupplier(() -> buildClient().doesBucketExist(bucketName));
    }

    @Override
    public Optional<BucketInfo> getBucketInfo(String bucketName){
        return queryOssOne(() -> buildClient().getBucketInfo(bucketName));
    }

    @Override
    public Bucket createBucket(CreateBucketRequest request){
        Bucket bucket = trySupplier(
                () -> buildClient().createBucket(request)
        );
        log.info("Aliyun oss bucket created. Bucket={}.", bucket.getName());
        return bucket;
    }

    @Override
    public void deleteBucket(String bucketName){
        if(doesBucketExist(bucketName)){
            tryRunnable(() -> buildClient().deleteBucket(bucketName));
            log.info("Aliyun oss bucket deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAccessControlList acl){
        tryRunnable(() -> buildClient().setBucketAcl(bucketName, acl));
        log.info("Aliyun oss bucket acl set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryOssOne(
                    () -> buildClient().getBucketVersioning(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration){
        tryRunnable(
                () -> buildClient().setBucketVersioning(
                        new SetBucketVersioningRequest(
                                bucketName, configuration
                        )
                )
        );
        log.info("Aliyun oss bucket versioning set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<ServerSideEncryptionConfiguration> describeBucketEncryption(String bucketName) {
        return queryOssOne(() -> buildClient().getBucketEncryption(bucketName));
    }

    @Override
    public void setBucketEncryption(SetBucketEncryptionRequest request){
        tryRunnable(() -> buildClient().setBucketEncryption(request));
        log.info("Aliyun oss bucket encryption set. Bucket={}.", request.getBucketName());
    }

    @Override
    public void deleteBucketEncryption(String bucketName){
        Optional<ServerSideEncryptionConfiguration> encryption = describeBucketEncryption(bucketName);

        if(encryption.isPresent()){
            tryRunnable(() -> buildClient().deleteBucketEncryption(bucketName));
            log.info("Aliyun oss bucket encryption deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketLoggingResult> describeBucketLogging(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryOssOne(
                    () -> buildClient().getBucketLogging(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketLogging(String bucketName, String targetBucket, String targetPrefix){
        tryRunnable(
                () -> buildClient().setBucketLogging(
                        new SetBucketLoggingRequest(
                                bucketName
                        ).withTargetBucket(
                                targetBucket
                        ).withTargetPrefix(
                                targetPrefix
                        )
                )
        );
        log.info("Aliyun oss bucket logging set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketLogging(String bucketName){
        Optional<BucketLoggingResult> logging = describeBucketLogging(bucketName);
        if(logging.isPresent()){
            tryRunnable(() -> buildClient().deleteBucketLogging(bucketName));
            log.info("Aliyun oss bucket logging deleted. Bucket={}.", bucketName);
        }

    }

    @Override
    public Optional<CORSConfiguration> describeBucketCors(String bucketName){
        return queryOssOne(() -> buildClient().getBucketCORS(new GenericRequest(bucketName)));
    }

    @Override
    public void setBucketCors(String bucketName, List<SetBucketCORSRequest.CORSRule> rules){
        SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
        request.setCorsRules(rules);
        tryRunnable(
                () -> buildClient().setBucketCORS(
                        request
                )
        );
        log.info("Aliyun oss bucket cors set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketCors(String bucketName){
        Optional<CORSConfiguration> configuration = describeBucketCors(bucketName);

        if(configuration.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketCORSRules(bucketName));
            log.info("Aliyun oss bucket cors deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public List<LifecycleRule> describeBucketLifecycle(String bucketName){
        return queryOssOne(() -> buildClient().getBucketLifecycle(bucketName)).orElseGet(List::of);
    }

    @Override
    public void setBucketLifecycle(String bucketName, List<LifecycleRule> rules){
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
        request.setLifecycleRules(rules);
        tryRunnable(() -> buildClient().setBucketLifecycle(request));
        log.info("Aliyun oss bucket lifecycle set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketLifecycle(String bucketName){
        List<LifecycleRule> lifecycleRules = describeBucketLifecycle(bucketName);

        if(!lifecycleRules.isEmpty()) {
            tryRunnable(() -> buildClient().deleteBucketLifecycle(bucketName));
            log.info("Aliyun oss bucket lifecycle deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<GetBucketPolicyResult> describeBucketPolicy(String bucketName){
        return queryOssOne(
                () -> buildClient().getBucketPolicy(bucketName)
        );
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText){
        tryRunnable(() -> buildClient().setBucketPolicy(bucketName, policyText));
        log.info("Aliyun oss bucket policy set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketPolicy(String bucketName){
        Optional<GetBucketPolicyResult> policy = describeBucketPolicy(bucketName);

        if(policy.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketPolicy(bucketName));
            log.info("Aliyun oss bucket policy deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketReferer> describeBucketReferer(String bucketName){
        return queryOssOne(
                () -> buildClient().getBucketReferer(bucketName)
        );
    }

    @Override
    public void setBucketReferer(String bucketName, BucketReferer referer){
        tryRunnable(() -> buildClient().setBucketReferer(bucketName, referer));
        log.info("Aliyun oss bucket referer set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketWebsiteResult> describeBucketWebsite(String bucketName){
        return queryOssOne(
                () -> buildClient().getBucketWebsite(bucketName)
        );
    }

    @Override
    public void setBucketWebsite(SetBucketWebsiteRequest request){
        tryRunnable(() -> buildClient().setBucketWebsite(request));
        log.info("Aliyun oss bucket website set. Bucket={}.", request.getBucketName());
    }

    @Override
    public void deleteBucketWebsite(String bucketName){
        var website = describeBucketWebsite(bucketName);

        if(website.isPresent()) {
            tryRunnable(() -> buildClient().deleteBucketWebsite(bucketName));
            log.info("Aliyun oss bucket website deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<AccessMonitor> describeAccessMonitor(String bucketName){
        return queryOssOne(() -> buildClient().getBucketAccessMonitor(bucketName));
    }

    @Override
    public void setAccessMonitor(String bucketName, boolean enabled){
        tryRunnable(
                () -> buildClient().putBucketAccessMonitor(
                        bucketName,
                        enabled ? "Enabled" : "Disabled"
                )
        );
        log.info("Aliyun oss bucket access monitor set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketStat> describeBucketStat(String bucketName){
        return queryOssOne(() -> buildClient().getBucketStat(bucketName));
    }
}
