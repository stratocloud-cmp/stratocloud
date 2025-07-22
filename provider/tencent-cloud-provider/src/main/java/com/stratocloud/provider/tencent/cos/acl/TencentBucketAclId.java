package com.stratocloud.provider.tencent.cos.acl;

import com.qcloud.cos.model.Grant;

public record TencentBucketAclId(String granteeType, String granteeIdentifier, String permission) {


    public TencentBucketAclId fromGrant(Grant grant){
        return new TencentBucketAclId(
                grant.getGrantee().getTypeIdentifier(),
                grant.getGrantee().getIdentifier(),
                grant.getPermission().toString()
        );
    }

    @Override
    public String toString() {
        return granteeType+"@"+granteeIdentifier+"@"+permission;
    }

    public static TencentBucketAclId fromString(String externalId){
        String[] split = externalId.split("@");
        return new TencentBucketAclId(split[0], split[1], split[2]);
    }
}
