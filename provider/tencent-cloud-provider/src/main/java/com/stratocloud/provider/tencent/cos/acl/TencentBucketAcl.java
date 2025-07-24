package com.stratocloud.provider.tencent.cos.acl;

import com.qcloud.cos.model.Permission;

import java.util.Set;

public record TencentBucketAcl(TencentBucketAclId id, Set<Permission> permissions) {
}
