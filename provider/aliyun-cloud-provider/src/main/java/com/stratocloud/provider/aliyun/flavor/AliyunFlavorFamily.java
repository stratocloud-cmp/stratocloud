package com.stratocloud.provider.aliyun.flavor;

import com.aliyun.ecs20140526.models.DescribeInstanceTypeFamiliesResponseBody;

public record AliyunFlavorFamily(
        DescribeInstanceTypeFamiliesResponseBody.DescribeInstanceTypeFamiliesResponseBodyInstanceTypeFamiliesInstanceTypeFamily detail
) {
}
