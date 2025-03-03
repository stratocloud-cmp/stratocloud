package com.stratocloud.provider.aliyun.lb.classic.acl;

import com.aliyun.slb20140515.models.DescribeAccessControlListsResponseBody;

public record AliyunClbAcl(
        DescribeAccessControlListsResponseBody.DescribeAccessControlListsResponseBodyAclsAcl detail
) {
}
