package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.BucketWebsiteResult;
import com.aliyun.oss.model.SetBucketWebsiteRequest;
import com.aliyun.oss.model.SubDirType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.Optional;

@Data
public class AliyunBucketUpdateWebsiteInput implements ResourceActionInput {
    @BooleanField(label = "启用静态网站")
    private boolean enabled;

    @InputField(label = "默认首页", required = false, conditions = "this.enabled === true")
    private String indexDocument;
    @BooleanField(label = "开通子目录首页", conditions = "this.enabled === true && this.indexDocument !== ''")
    private boolean supportSubDir;
    @SelectField(
            label = "文件404规则",
            options = {
                    "Redirect",
                    "NoSuchKey",
                    "Index"
            },
            optionNames = {
                    "Redirect",
                    "NoSuchKey",
                    "Index"
            },
            defaultValues = "Redirect",
            conditions = "this.enabled === true && this.indexDocument !== '' && this.supportSubDir === true"
    )
    private String subDirType;
    @InputField(label = "默认404页", required = false, conditions = "this.enabled === true")
    private String errorDocument;
    @SelectField(
            label = "错误文档响应码",
            options = {
                    "404",
                    "200"
            },
            optionNames = {
                    "404",
                    "200"
            },
            defaultValues = "404",
            conditions = "this.enabled === true && this.errorDocument !== ''"
    )
    private String httpStatus;

    public static AliyunBucketUpdateWebsiteInput getInput(AliyunClient client,
                                                          String bucketName){
        AliyunOssService ossService = client.oss();

        Optional<BucketWebsiteResult> website = ossService.describeBucketWebsite(bucketName);

        AliyunBucketUpdateWebsiteInput input = new AliyunBucketUpdateWebsiteInput();

        if(website.isPresent()){
            input.setEnabled(true);

            input.setIndexDocument(website.get().getIndexDocument());
            input.setSupportSubDir(website.get().isSupportSubDir());
            input.setSubDirType(website.get().getSubDirType());

            input.setErrorDocument(website.get().getErrorDocument());
            input.setHttpStatus(website.get().getHttpStatus());
        }else {
            input.setEnabled(false);
        }

        return input;
    }

    @JsonIgnore
    public void apply(AliyunClient client, String bucketName){
        AliyunOssService ossService = client.oss();

        Optional<BucketWebsiteResult> website = ossService.describeBucketWebsite(bucketName);

        if(isEnabled()){
            SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName);

            website.ifPresent(w -> request.setRoutingRules(w.getRoutingRules()));
            request.setIndexDocument(indexDocument);
            if(Utils.isNotBlank(indexDocument)) {
                request.setSupportSubDir(supportSubDir);
                if(supportSubDir && Utils.isNotBlank(subDirType))
                    request.setSubDirType(SubDirType.parse(subDirType));
            }

            request.setErrorDocument(errorDocument);
            if(Utils.isNotBlank(errorDocument))
                request.setHttpStatus(httpStatus);

            ossService.setBucketWebsite(request);
        } else {
            if(website.isPresent() && Utils.isNotEmpty(website.get().getRoutingRules()))
                throw new BadCommandException(
                        "Cannot disable website because bucket website routing rules exist, " +
                                "otherwise it will be overridden. " +
                                "Configuration for routing rules is not supported yet."
                );

            ossService.deleteBucketWebsite(bucketName);
        }
    }
}
