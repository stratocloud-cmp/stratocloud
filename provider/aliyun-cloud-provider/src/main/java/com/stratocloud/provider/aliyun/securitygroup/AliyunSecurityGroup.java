package com.stratocloud.provider.aliyun.securitygroup;

import com.aliyun.ecs20140526.models.DescribeSecurityGroupsResponseBody;

public record AliyunSecurityGroup(
        DescribeSecurityGroupsResponseBody.DescribeSecurityGroupsResponseBodySecurityGroupsSecurityGroup detail
) {

}
