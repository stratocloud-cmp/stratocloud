package com.stratocloud.provider.aliyun.flavor;

import com.aliyun.ecs20140526.models.DescribeInstanceTypesResponseBody;
import com.stratocloud.resource.ResourceState;

public record AliyunFlavor(AliyunFlavorId flavorId,
                           DescribeInstanceTypesResponseBody.DescribeInstanceTypesResponseBodyInstanceTypesInstanceType detail,
                           ResourceState state) {

}
