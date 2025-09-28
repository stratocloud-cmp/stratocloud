package com.stratocloud.provider.aliyun.rds.model;

import com.aliyun.rds20140815.models.DescribeParameterGroupResponseBody;

public record RdsParamGroup(DescribeParameterGroupResponseBody.DescribeParameterGroupResponseBodyParamGroupParameterGroup detail) {
}
