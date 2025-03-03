package com.stratocloud.provider.aliyun.image;

import com.aliyun.ecs20140526.models.DescribeImagesResponseBody;

public record AliyunImage(
        DescribeImagesResponseBody.DescribeImagesResponseBodyImagesImage detail
) {
}
