package com.stratocloud.provider.tencent.cos.session;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRule;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleId;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRule;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleId;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class CosSessionImpl implements CosSession {

    private final COSClient cosClient;

    public CosSessionImpl(COSClient cosClient) {
        this.cosClient = cosClient;
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
        }catch (CosClientException e){
            log.warn("CosErrorCode: {}", e.getErrorCode());

            if(e instanceof CosServiceException serviceException) {
                if(serviceException.getStatusCode() == HttpStatus.NOT_FOUND.value())
                    throw new ExternalResourceNotFoundException(e.getMessage(), e);

                boolean retrying = Set.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.value()
                ).contains(serviceException.getStatusCode());

                if(retrying && e.isRetryable()){
                    log.warn("Retrying cos request later: {}", e.getMessage());
                    SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                    return doTrySupplier(supplier, triedTimes+1);
                }
            }

            throw new StratoException(e.getMessage(), e);
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private <R> Optional<R> queryCosOne(Supplier<R> supplier){
        try {
            return Optional.ofNullable(trySupplier(supplier));
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.getMessage());
            return Optional.empty();
        }
    }


    private <R, E> List<E> queryCosAll(Supplier<R> supplier,
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
        ListBucketsRequest request = new ListBucketsRequest();
        return queryCosAll(
                () -> cosClient.getService(request),
                ListBucketsResult::getBuckets,
                ListBucketsResult::getNextMarker,
                request::setMarker
        ).stream().filter(
                b -> Objects.equals(cosClient.getClientConfig().getRegion().getRegionName(), b.getLocation())
        ).toList();
    }

    @Override
    public Optional<Bucket> describeBucket(String bucketName){
        return describeBuckets().stream().filter(
                b -> Objects.equals(b.getName(), bucketName)
        ).findAny();
    }

    @Override
    public List<COSVersionSummary> describeObjectVersions(String bucketName){
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        return queryCosAll(
                () -> cosClient.listVersions(request),
                VersionListing::getVersionSummaries,
                VersionListing::getNextKeyMarker,
                request::setKeyMarker
        );
    }

    @Override
    public void deleteObjects(String bucketName, List<DeleteObjectsRequest.KeyVersion> keyVersions){
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setKeys(keyVersions);
        tryRunnable(() -> cosClient.deleteObjects(request));
    }

    @Override
    public boolean doesBucketExist(String bucketName){
        return trySupplier(() -> cosClient.doesBucketExist(bucketName));
    }

    @Override
    public Optional<HeadBucketResult> headBucket(String bucketName){
        return queryCosOne(() -> cosClient.headBucket(new HeadBucketRequest(bucketName)));
    }

    @Override
    public Bucket createBucket(CreateBucketRequest request, boolean multiAz){
        Bucket bucket = trySupplier(
                () -> multiAz ? cosClient.createMAZBucket(request) : cosClient.createBucket(request)
        );
        log.info("Tencent cos bucket created. Bucket={}.", bucket.getName());
        return bucket;
    }

    @Override
    public void deleteBucket(String bucketName){
        if(doesBucketExist(bucketName)){
            trySupplier(() -> {
                cosClient.deleteBucket(bucketName);
                return true;
            });
            log.info("Tencent cos bucket deleted. Bucket={}.", bucketName);
        }


    }

    @Override
    public Optional<AccessControlList> describeBucketAcl(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryCosOne(() -> cosClient.getBucketAcl(bucketName));
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList accessControlList){
        tryRunnable(() -> cosClient.setBucketAcl(bucketName, accessControlList));
        log.info("Tencent cos bucket acl set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketVersioningConfiguration> describeBucketVersioning(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryCosOne(
                    () -> cosClient.getBucketVersioningConfiguration(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketVersioning(String bucketName, BucketVersioningConfiguration configuration){
        tryRunnable(
                () -> cosClient.setBucketVersioningConfiguration(
                        new SetBucketVersioningConfigurationRequest(
                                bucketName, configuration
                        )
                )
        );
        log.info("Tencent cos bucket versioning set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketLoggingConfiguration> describeBucketLogging(String bucketName){
        if(doesBucketExist(bucketName)){
            return queryCosOne(
                    () -> cosClient.getBucketLoggingConfiguration(bucketName)
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setBucketLogging(String bucketName, BucketLoggingConfiguration configuration){
        tryRunnable(
                () -> cosClient.setBucketLoggingConfiguration(
                        new SetBucketLoggingConfigurationRequest(
                                bucketName, configuration
                        )
                )
        );
        log.info("Tencent cos bucket logging set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketIntelligentTierConfiguration> describeDefaultBucketIntelligentTier(String bucketName){
        if(doesBucketExist(bucketName)){
            var request = new GetBucketIntelligentTierConfigurationRequest(bucketName);
            request.putCustomQueryParameter("id", "default");
            return queryCosOne(
                    () -> cosClient.getBucketIntelligentTierConfiguration(
                            request
                    )
            );
        }else {
            return Optional.empty();
        }
    }

    @Override
    public void setDefaultBucketIntelligentTier(String bucketName, BucketIntelligentTierConfiguration configuration){
        var request = new SetBucketIntelligentTierConfigurationRequest(bucketName, configuration);
        request.putCustomQueryParameter("id", "default");
        tryRunnable(
                () -> cosClient.setBucketIntelligentTieringConfiguration(
                        request
                )
        );
        log.info("Tencent cos bucket default intelligent tier set. Bucket={}.", bucketName);
    }

    @Override
    public List<TencentBucketCorsRule> describeBucketCorsRules() {
        List<Bucket> buckets = describeBuckets();

        List<TencentBucketCorsRule> result = new ArrayList<>();

        for (Bucket bucket : buckets) {
            result.addAll(describeBucketCorsRulesByBucket(bucket.getName()));
        }

        return result;
    }

    @Override
    public Optional<TencentBucketCorsRule> describeBucketCorsRule(TencentBucketCorsRuleId ruleId){
        return describeBucketCorsRulesByBucket(ruleId.bucketName()).stream().filter(
                r -> r.id().equals(ruleId)
        ).findAny();
    }

    @Override
    public List<TencentBucketCorsRule> describeBucketCorsRulesByBucket(String bucketName){
        Optional<BucketCrossOriginConfiguration> configuration = queryCosOne(
                () -> cosClient.getBucketCrossOriginConfiguration(bucketName)
        );

        if(configuration.isEmpty())
            return List.of();

        List<CORSRule> rules = configuration.get().getRules();

        if(Utils.isEmpty(rules))
            return List.of();

        return rules.stream().filter(
                r -> Utils.isNotBlank(r.getId())
        ).map(
                r -> new TencentBucketCorsRule(
                        new TencentBucketCorsRuleId(bucketName, r.getId()),
                        r
                )
        ).toList();
    }

    @Override
    public void setBucketCors(String bucketName, BucketCrossOriginConfiguration configuration){
        tryRunnable(() -> cosClient.setBucketCrossOriginConfiguration(bucketName, configuration));
        log.info("Tencent cos bucket cors set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketCors(String bucketName){
        Optional<BucketCrossOriginConfiguration> configuration = queryCosOne(
                () -> cosClient.getBucketCrossOriginConfiguration(bucketName)
        );

        if(configuration.isPresent()) {
            tryRunnable(() -> cosClient.deleteBucketCrossOriginConfiguration(bucketName));
            log.info("Tencent cos bucket cors deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public List<TencentBucketLifecycleRule> describeBucketLifecycleRules(){
        List<Bucket> buckets = describeBuckets();

        List<TencentBucketLifecycleRule> result = new ArrayList<>();

        for (Bucket bucket : buckets) {
            result.addAll(describeBucketLifecycleRulesByBucket(bucket.getName()));
        }

        return result;
    }

    @Override
    public Optional<TencentBucketLifecycleRule> describeBucketLifecycleRule(TencentBucketLifecycleRuleId ruleId){
        return describeBucketLifecycleRulesByBucket(
                ruleId.bucketName()
        ).stream().filter(
                r -> Objects.equals(r.id(), ruleId)
        ).findAny();
    }

    @Override
    public List<TencentBucketLifecycleRule> describeBucketLifecycleRulesByBucket(String bucketName){
        Optional<BucketLifecycleConfiguration> configuration = queryCosOne(
                () -> cosClient.getBucketLifecycleConfiguration(bucketName)
        );

        if(configuration.isEmpty())
            return List.of();

        List<BucketLifecycleConfiguration.Rule> rules = configuration.get().getRules();

        if(Utils.isEmpty(rules))
            return List.of();

        return rules.stream().filter(
                r -> Utils.isNotBlank(r.getId())
        ).map(
                r -> new TencentBucketLifecycleRule(
                        new TencentBucketLifecycleRuleId(bucketName, r.getId()),
                        r
                )
        ).toList();
    }

    @Override
    public void setBucketLifecycle(String bucketName, BucketLifecycleConfiguration configuration){
        tryRunnable(() -> cosClient.setBucketLifecycleConfiguration(bucketName, configuration));
        log.info("Tencent cos bucket lifecycle set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketLifecycle(String bucketName){
        Optional<BucketLifecycleConfiguration> configuration = queryCosOne(
                () -> cosClient.getBucketLifecycleConfiguration(bucketName)
        );

        if(configuration.isPresent()) {
            tryRunnable(() -> cosClient.deleteBucketLifecycleConfiguration(bucketName));
            log.info("Tencent cos bucket lifecycle deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketPolicy> describeBucketPolicy(String bucketName){
        return queryCosOne(
                () -> cosClient.getBucketPolicy(bucketName)
        );
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText){
        tryRunnable(() -> cosClient.setBucketPolicy(bucketName, policyText));
        log.info("Tencent cos bucket policy set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketPolicy(String bucketName){
        Optional<BucketPolicy> policy = describeBucketPolicy(bucketName);

        if(policy.isPresent()) {
            tryRunnable(() -> cosClient.deleteBucketPolicy(bucketName));
            log.info("Tencent cos bucket policy deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public Optional<BucketRefererConfiguration> describeBucketReferer(String bucketName){
        return queryCosOne(
                () -> cosClient.getBucketRefererConfiguration(bucketName)
        );
    }

    @Override
    public void setBucketReferer(String bucketName, BucketRefererConfiguration configuration){
        tryRunnable(() -> cosClient.setBucketRefererConfiguration(bucketName, configuration));
        log.info("Tencent cos bucket referer set. Bucket={}.", bucketName);
    }

    @Override
    public Optional<BucketWebsiteConfiguration> describeBucketWebsite(String bucketName){
        return queryCosOne(
                () -> cosClient.getBucketWebsiteConfiguration(bucketName)
        );
    }

    @Override
    public void setBucketWebsite(String bucketName, BucketWebsiteConfiguration configuration){
        tryRunnable(() -> cosClient.setBucketWebsiteConfiguration(bucketName, configuration));
        log.info("Tencent cos bucket website set. Bucket={}.", bucketName);
    }

    @Override
    public void deleteBucketWebsite(String bucketName){
        Optional<BucketWebsiteConfiguration> configuration = describeBucketWebsite(bucketName);

        if(configuration.isPresent()) {
            tryRunnable(() -> cosClient.deleteBucketWebsiteConfiguration(bucketName));
            log.info("Tencent cos bucket website deleted. Bucket={}.", bucketName);
        }
    }

    @Override
    public void shutdown() {
        cosClient.shutdown();
    }
}
