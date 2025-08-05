package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.BucketReferer;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunBucketUpdateRefererInput implements ResourceActionInput {
    @BooleanField(label = "允许空Referer访问")
    private boolean allowEmptyReferer;
    @BooleanField(label = "截断URL中的QueryString")
    private boolean allowTruncateQueryString;

    @SelectField(label = "Referer白名单")
    private List<String> refererList;
    @SelectField(label = "Referer黑名单")
    private List<String> blackRefererList;

    public static AliyunBucketUpdateRefererInput getInput(AliyunClient client, String bucketName){
        BucketReferer referer = client.oss().describeBucketReferer(
                bucketName
        ).orElseThrow(
                () -> new StratoException("Referer config not found")
        );

        AliyunBucketUpdateRefererInput t = new AliyunBucketUpdateRefererInput();
        t.setAllowEmptyReferer(referer.isAllowEmptyReferer());
        t.setAllowTruncateQueryString(
                referer.isAllowTruncateQueryString() != null ? referer.isAllowTruncateQueryString():false
        );
        t.setRefererList(referer.getRefererList());
        t.setBlackRefererList(referer.getBlackRefererList());

        return t;
    }
}
