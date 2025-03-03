package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup;

import com.aliyun.slb20140515.models.DescribeVServerGroupAttributeResponseBody;
import com.aliyun.slb20140515.models.DescribeVServerGroupsResponseBody;

public record AliyunClbServerGroup(
        AliyunClbServerGroupId id,
        DescribeVServerGroupsResponseBody.DescribeVServerGroupsResponseBodyVServerGroupsVServerGroup detail,
        DescribeVServerGroupAttributeResponseBody attributes
) {
}
