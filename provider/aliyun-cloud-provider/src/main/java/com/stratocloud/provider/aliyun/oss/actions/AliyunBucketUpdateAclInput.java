package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.CannedAccessControlList;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunBucketUpdateAclInput implements ResourceActionInput {
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
}
