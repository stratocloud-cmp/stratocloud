package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.AccessControlList;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.huawei.obs.HuaweiBucketAclType;
import com.stratocloud.provider.huawei.obs.HuaweiBucketSpec;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HuaweiBucketBuildInput extends HuaweiBucketSpec implements ResourceActionInput {
    @SelectField(
            label = "读写权限",
            options = {
                    HuaweiBucketAclType.PRIVATE,
                    HuaweiBucketAclType.PUBLIC_READ,
                    HuaweiBucketAclType.PUBLIC_READ_WRITE,
                    HuaweiBucketAclType.PUBLIC_READ_DELIVERED,
                    HuaweiBucketAclType.PUBLIC_READ_WRITE_DELIVERED,
                    HuaweiBucketAclType.PUBLIC_OWNER_FULL_CONTROLLED
            },
            optionNames = {
                    "私有",
                    "公共读",
                    "公共读写",
                    "公共读(包括对象内容)",
                    "公共读写(包括对象内容)",
                    "桶和对象的所有者拥有对象的完全控制权限"
            },
            defaultValues = HuaweiBucketAclType.PRIVATE
    )
    private String aclType;

    public AccessControlList getCannedACL() {
        if(aclType == null)
            return AccessControlList.REST_CANNED_PRIVATE;

        return switch (aclType){
            case HuaweiBucketAclType.PUBLIC_READ ->
                    AccessControlList.REST_CANNED_PUBLIC_READ;
            case HuaweiBucketAclType.PUBLIC_READ_WRITE ->
                    AccessControlList.REST_CANNED_PUBLIC_READ_WRITE;
            case HuaweiBucketAclType.PUBLIC_READ_DELIVERED ->
                    AccessControlList.REST_CANNED_PUBLIC_READ_DELIVERED;
            case HuaweiBucketAclType.PUBLIC_READ_WRITE_DELIVERED ->
                    AccessControlList.REST_CANNED_PUBLIC_READ_WRITE_DELIVERED;
            case HuaweiBucketAclType.PUBLIC_OWNER_FULL_CONTROLLED ->
                    AccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL;
            default ->
                    AccessControlList.REST_CANNED_PRIVATE;
        };
    }
}
