package com.stratocloud.provider.aliyun.eip;

import com.aliyun.vpc20160428.models.DescribeCommonBandwidthPackagesResponseBody;

public record AliyunBandwidthPackage(
        DescribeCommonBandwidthPackagesResponseBody.DescribeCommonBandwidthPackagesResponseBodyCommonBandwidthPackagesCommonBandwidthPackage detail
) {
}
