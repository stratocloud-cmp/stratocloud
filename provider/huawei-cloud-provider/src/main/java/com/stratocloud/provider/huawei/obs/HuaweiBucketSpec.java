package com.stratocloud.provider.huawei.obs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.obs.services.model.*;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.utils.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Data
public class HuaweiBucketSpec {
    @SelectField(
            label = "存储类型",
            options = {
                    "OBJECT",
                    "PFS"
            },
            optionNames = {
                    "对象存储桶",
                    "并行文件系统"
            },
            defaultValues = "OBJECT"
    )
    private BucketTypeEnum bucketType;
    @SelectField(
            label = "存储类型",
            options = {
                    "STANDARD",
                    "WARM",
                    "COLD",
                    "DEEP_ARCHIVE"
            },
            optionNames = {
                    "标准存储",
                    "低频访问存储",
                    "归档存储",
                    "深度冷归档存储"
            },
            defaultValues = "STANDARD"
    )
    private StorageClassEnum storageClass;

    @BooleanField(label = "多AZ特性")
    private boolean enableMultiAz;

    @BooleanField(label = "版本控制")
    private boolean enableVersioning;

    @BooleanField(label = "服务端加密")
    private boolean enableEncryption;

    @SelectField(
            label = "加密方式",
            options = {
                    "AES256",
                    "KMS"
            },
            optionNames = {
                    "AES256",
                    "KMS"
            },
            defaultValues = "AES256",
            conditions = "this.enableEncryption === true"
    )
    private SSEAlgorithmEnum sseAlgorithm;

    @InputField(
            label = "KMSMasterKeyID",
            required = false,
            conditions = "this.enableEncryption === true && this.sseAlgorithm === 'KMS'"
    )
    private String kmsMasterKeyId;


    public static <T extends HuaweiBucketSpec> T getSpec(HuaweiCloudClient client,
                                                         String bucketName,
                                                         Supplier<T> specConstructor){
        if(Utils.isBlank(bucketName))
            throw new StratoException("Bucket name not provided");

        HuaweiObsService obsService = client.obs();
        BucketMetadataInfoResult bucket = obsService.getBucketInfo(bucketName).orElseThrow(
                () -> new StratoException("Bucket not found")
        );

        T t = specConstructor.get();

        t.setBucketType(bucket.getBucketType());
        t.setStorageClass(bucket.getBucketStorageClass());
        t.setEnableMultiAz(bucket.getAvailableZone() == AvailableZoneEnum.MULTI_AZ);

        Optional<BucketEncryption> sseConfig = obsService.describeBucketEncryption(bucketName);
        if(sseConfig.isPresent()){
            SSEAlgorithmEnum sseAlgorithmEnum = sseConfig.get().getSseAlgorithm();

            if(sseAlgorithmEnum != null){
                t.setEnableEncryption(true);
                t.setSseAlgorithm(sseAlgorithmEnum);
                t.setKmsMasterKeyId(sseConfig.get().getKmsKeyId());
            }
        }

        Optional<BucketVersioningConfiguration> versioning = obsService.describeBucketVersioning(bucketName);

        if(versioning.isPresent())
            if(versioning.get().getVersioningStatus() == VersioningStatusEnum.ENABLED)
                t.setEnableVersioning(true);

        return t;
    }

    @JsonIgnore
    public void applyStorageClassQuietly(HuaweiCloudClient client, String bucketName){
        try {
            client.obs().setBucketStorageClass(bucketName, storageClass);
        }catch (Exception e){
            log.warn("Failed to apply bucket storage class. Bucket={}.", bucketName, e);
        }
    }

    @JsonIgnore
    public void applyVersioningQuietly(HuaweiCloudClient client, String bucketName){
        try {
            BucketVersioningConfiguration configuration = new BucketVersioningConfiguration(
                    isEnableVersioning() ?
                            VersioningStatusEnum.ENABLED : VersioningStatusEnum.SUSPENDED
            );
            client.obs().setBucketVersioning(
                    bucketName, configuration
            );
        }catch (Exception e){
            log.warn("Failed to apply bucket versioning. Bucket={}.", bucketName, e);
        }
    }

    @JsonIgnore
    public void applyEncryptionQuietly(HuaweiCloudClient client, String bucketName){
        try {
            HuaweiObsService obsService = client.obs();

            if(isEnableEncryption()){
                BucketEncryption bucketEncryption = new BucketEncryption(sseAlgorithm);

                if(sseAlgorithm == SSEAlgorithmEnum.KMS)
                    bucketEncryption.setKmsKeyId(kmsMasterKeyId);

                SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(
                        bucketName,
                        bucketEncryption
                );

                obsService.setBucketEncryption(request);
            } else {
                obsService.deleteBucketEncryption(bucketName);
            }
        }catch (Exception e){
            log.warn("Failed to apply bucket encryption. Bucket={}.", bucketName, e);
        }
    }
}
