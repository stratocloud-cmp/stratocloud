package com.stratocloud.provider.tencent.cos.acl;

import com.qcloud.cos.model.Grantee;

public record TencentBucketAclId(String bucketName,
                                 String granteeType,
                                 String granteeIdentifier) {


    public static TencentBucketAclId fromGrantee(String bucketName, Grantee grantee){
        return new TencentBucketAclId(
                bucketName,
                grantee.getTypeIdentifier(),
                grantee.getIdentifier()
        );
    }

    @Override
    public String toString() {
        return granteeIdentifier+"@"+granteeType+"@"+bucketName;
    }

    public static TencentBucketAclId fromString(String externalId){
        String[] split = externalId.split("@");
        return new TencentBucketAclId(split[2], split[1], split[0]);
    }
}
