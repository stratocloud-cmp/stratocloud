package com.stratocloud.provider.aliyun.oss;

import com.aliyun.oss.model.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.utils.Utils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Data
public class AliyunBucketSpec {
    @SelectField(
            label = "读写权限",
            options = {
                    "Private",
                    "PublicRead",
                    "PublicReadWrite"
            },
            optionNames = {
                    "私有",
                    "公共读",
                    "公共读写"
            },
            defaultValues = "Private"
    )
    private CannedAccessControlList aclType;

    @SelectField(
            label = "存储类型",
            options = {
                    "Standard",
                    "IA",
                    "Archive",
                    "ColdArchive",
                    "DeepColdArchive"
            },
            optionNames = {
                    "标准存储",
                    "低频访问存储",
                    "归档存储",
                    "冷归档存储",
                    "深度冷归档存储"
            },
            defaultValues = "Standard"
    )
    private StorageClass storageClass;

    @SelectField(
            label = "数据容灾类型",
            options = {
                    "LRS",
                    "ZRS"
            },
            optionNames = {
                    "本地冗余",
                    "同城冗余"
            },
            defaultValues = "LRS"
    )
    private DataRedundancyType redundancyType;

    @BooleanField(label = "版本控制")
    private boolean enableVersioning;

    @BooleanField(label = "服务端加密")
    private boolean enableEncryption;

    @SelectField(
            label = "加密方式",
            options = {
                    "AES256",
                    "SM4",
                    "KMS"
            },
            optionNames = {
                    "AES256",
                    "SM4",
                    "KMS"
            },
            defaultValues = "AES256",
            conditions = "this.enableEncryption === true"
    )
    private String sseAlgorithm;

    @SelectField(
            label = "KMS数据加密算法",
            options = {
                    "AES256",
                    "SM4"
            },
            optionNames = {
                    "AES256",
                    "SM4"
            },
            defaultValues = "AES256",
            conditions = "this.enableEncryption === true && this.sseAlgorithm === 'KMS'"
    )
    private String kmsDataEncryption;

    @InputField(
            label = "KMSMasterKeyID",
            required = false,
            conditions = "this.enableEncryption === true && this.sseAlgorithm === 'KMS'"
    )
    private String kmsMasterKeyId;


    public static <T extends AliyunBucketSpec> T getSpec(AliyunClient client,
                                                         String bucketName,
                                                         Supplier<T> specConstructor){
        if(Utils.isBlank(bucketName))
            throw new StratoException("Bucket name not provided");

        AliyunOssService ossService = client.oss();
        BucketInfo bucket = ossService.getBucketInfo(bucketName).orElseThrow(
                () -> new StratoException("Bucket not found")
        );

        T t = specConstructor.get();

        t.setAclType(bucket.getCannedACL());
        t.setStorageClass(bucket.getBucket().getStorageClass());
        t.setRedundancyType(bucket.getDataRedundancyType());

        ServerSideEncryptionConfiguration sseConfig = bucket.getServerSideEncryptionConfiguration();
        if(sseConfig != null && sseConfig.getApplyServerSideEncryptionByDefault() != null){
            var encryption = sseConfig.getApplyServerSideEncryptionByDefault();

            if(Utils.isNotBlank(encryption.getSSEAlgorithm())){
                if(!"None".equals(encryption.getSSEAlgorithm())){
                    t.setEnableEncryption(true);
                    t.setSseAlgorithm(encryption.getSSEAlgorithm());
                    t.setKmsDataEncryption(encryption.getKMSDataEncryption());
                    t.setKmsMasterKeyId(encryption.getKMSMasterKeyID());
                }
            }
        }

        Optional<BucketVersioningConfiguration> versioning = ossService.describeBucketVersioning(bucketName);

        if(versioning.isPresent())
            if(BucketVersioningConfiguration.ENABLED.equals(versioning.get().getStatus()))
                t.setEnableVersioning(true);

        return t;
    }

    @JsonIgnore
    public void applyVersioningQuietly(AliyunClient client, String bucketName){
        try {
            BucketVersioningConfiguration configuration = new BucketVersioningConfiguration(
                    isEnableVersioning() ?
                            BucketVersioningConfiguration.ENABLED : BucketVersioningConfiguration.SUSPENDED
            );
            client.oss().setBucketVersioning(
                    bucketName, configuration
            );
        }catch (Exception e){
            log.warn("Failed to apply bucket versioning. Bucket={}.", bucketName, e);
        }
    }

    @JsonIgnore
    public void applyEncryptionQuietly(AliyunClient client, String bucketName){
        try {
            AliyunOssService ossService = client.oss();
            if(isEnableEncryption()){
                SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(bucketName);
                ServerSideEncryptionByDefault encryption = new ServerSideEncryptionByDefault();
                encryption.setSSEAlgorithm(sseAlgorithm);

                if(Objects.equals(sseAlgorithm, "KMS")){
                    if(Objects.equals(kmsDataEncryption, "SM4"))
                        encryption.setKMSDataEncryption(kmsDataEncryption);
                    if(Utils.isNotBlank(kmsMasterKeyId))
                        encryption.setKMSMasterKeyID(kmsMasterKeyId);
                }

                request.withServerSideEncryptionConfiguration(
                        new ServerSideEncryptionConfiguration().withApplyServerSideEncryptionByDefault(
                                encryption
                        )
                );
                ossService.setBucketEncryption(request);
            } else {
                ossService.deleteBucketEncryption(bucketName);
            }
        }catch (Exception e){
            log.warn("Failed to apply bucket encryption. Bucket={}.", bucketName, e);
        }
    }
}
