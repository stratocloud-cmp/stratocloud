package com.stratocloud.provider.tencent.cos.bucket;

import com.qcloud.cos.model.*;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.resource.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Data
public class TencentBucketSpec {

    private CannedAccessControlList aclType = CannedAccessControlList.Default;
    private boolean enableMultiAz;
    private boolean enableVersioning;
    private boolean enableIntelligentTier;
    private int defaultIntelligentTierDays = 30;

    public static TencentBucketSpec retrieveFrom(CosSession cosSession, Resource bucketResource){
        TencentBucketSpec bucketSpec = new TencentBucketSpec();

        String bucketName = bucketResource.getExternalId();

        Optional<AccessControlList> aclOptional = cosSession.describeBucketAcl(bucketName);
        aclOptional.ifPresent(
                acl -> bucketSpec.setAclType(acl.getCannedAccessControl())
        );

        Optional<HeadBucketResult> headBucketResult = cosSession.headBucket(bucketName);
        headBucketResult.ifPresent(head -> bucketSpec.setEnableMultiAz(head.isMazBucket()));

        var versioningConfigurationOptional = cosSession.describeBucketVersioning(bucketName);
        versioningConfigurationOptional.ifPresent(
                configuration -> bucketSpec.setEnableVersioning(
                        Objects.equals(
                                configuration.getStatus(),
                                BucketVersioningConfiguration.ENABLED
                        )
                )
        );

        var intelligentTierConfigurationOptional = cosSession.describeDefaultBucketIntelligentTier(bucketName);
        intelligentTierConfigurationOptional.ifPresent(
                configuration -> {
                    bucketSpec.setEnableIntelligentTier(
                            Objects.equals(
                                    configuration.getStatus(),
                                    BucketIntelligentTierConfiguration.ENABLED
                            )
                    );
                    if(configuration.getTransition() != null){
                        int days = configuration.getTransition().getDays();
                        bucketSpec.setDefaultIntelligentTierDays(
                                days > 0 ? days : 30
                        );
                    }
                }
        );

        return bucketSpec;
    }

    public void applyVersioningQuietly(CosSession cosSession, String bucketName){
        try {
            var versioningConfiguration = cosSession.describeBucketVersioning(bucketName);

            String currentStatus = versioningConfiguration.isPresent() ? versioningConfiguration.get().getStatus() :
                    BucketVersioningConfiguration.OFF;

            if(Set.of(BucketVersioningConfiguration.OFF, BucketVersioningConfiguration.SUSPENDED).contains(currentStatus)){
                if(isEnableVersioning())
                    cosSession.setBucketVersioning(
                            bucketName,
                            new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)
                    );
            } else if(Objects.equals(BucketVersioningConfiguration.ENABLED, currentStatus)){
                if(!isEnableVersioning())
                    cosSession.setBucketVersioning(
                            bucketName,
                            new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED)
                    );
            } else {
                log.warn("Unknown versioning status: {}", currentStatus);
            }
        }catch (Exception e){
            log.warn("Failed to apply bucket versioning", e);
        }
    }

    public void applyIntelligentTierQuietly(CosSession cosSession,
                                            String bucketName){
        try {
            var configuration = cosSession.describeDefaultBucketIntelligentTier(bucketName);

            String currentStatus = configuration.isPresent() ? configuration.get().getStatus() :
                    BucketIntelligentTierConfiguration.SUSPENDED;

            if(Objects.equals(currentStatus, BucketIntelligentTierConfiguration.SUSPENDED)){
                if(isEnableIntelligentTier()){
                    BucketIntelligentTierConfiguration newConfiguration = new BucketIntelligentTierConfiguration();
                    newConfiguration.setStatus(BucketIntelligentTierConfiguration.ENABLED);
                    var transition = new BucketIntelligentTierConfiguration.Transition(defaultIntelligentTierDays);

                    newConfiguration.setTransition(transition);
                    cosSession.setDefaultBucketIntelligentTier(
                            bucketName,
                            newConfiguration
                    );
                }
            } else {
                log.warn("Cannot modify default intelligent tier configuration.");
            }
        }catch (Exception e){
            log.warn("Failed to apply bucket intelligent tier", e);
        }
    }
}
